package com.multimodel.llm.controller;

import com.multimodel.llm.exception.InvalidAnswerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * REST controller exposing chat endpoints whose responses are self-evaluated for factual
 * accuracy using a {@link FactCheckingEvaluator} (backed by the Bespoke-Minicheck model)
 * before being returned, rejecting answers that fail the check.
 */
@RestController
@RequestMapping("/api/evaluate")
public class SelfEvaluatingChatController {

    private final ChatClient ollamaChatClient;
    private final FactCheckingEvaluator factCheckingEvaluator;

    /**
     * HR policy template used as both the system prompt and fact-checking context in
     * {@link #promptStuff}.
     */
    @Value("classpath:/promptTemplates/hrPolicyTemplate.st")
    Resource hrPolicyTemplate;

    /**
     * Creates a new controller backed by the given chat clients.
     *
     * @param ollamaChatClient chat client used to generate responses
     * @param bespokeMinicheckChatClient chat client used to build the fact-checking evaluator
     */
    public SelfEvaluatingChatController(
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
            @Qualifier("bespokeMinicheckChatClient") ChatClient bespokeMinicheckChatClient) {
        this.ollamaChatClient = ollamaChatClient;
        this.factCheckingEvaluator = FactCheckingEvaluator
                .builder(bespokeMinicheckChatClient.mutate()).build();
    }

    /**
     * Sends the given message to the model and validates the response for factual accuracy
     * with no supporting context before returning it.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response text
     * @throws com.multimodel.llm.exception.InvalidAnswerException if the response fails the fact-checking evaluation
     */
    @RequestMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        String aiResponse = ollamaChatClient
                .prompt(message)
                .call().content();
        return validateAnswer(message, aiResponse, List.of());
    }

    /**
     * Answers the given message with the HR policy document stuffed into the system prompt,
     * then validates the response against that same document as fact-checking context before
     * returning it.
     *
     * @param message the user's question, bound from the {@code message} query parameter
     * @return the model's response text
     * @throws UncheckedIOException if the HR policy template content cannot be read
     * @throws com.multimodel.llm.exception.InvalidAnswerException if the response fails the fact-checking evaluation
     */
    @RequestMapping("/prompt-stuffing")
    public String promptStuff(@RequestParam("message") String message) {
        String aiResponse = ollamaChatClient
                .prompt(message)
                .system(hrPolicyTemplate)
                .user(message)
                .call()
                .content();
        try {
            String hrPolicyContent = hrPolicyTemplate.getContentAsString(StandardCharsets.UTF_8);
            validateAnswer(message, aiResponse, List.of(new Document(hrPolicyContent)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return validateAnswer(message, aiResponse, List.of());
    }

    /**
     * Validates the given answer against the supplied context using the fact-checking
     * evaluator, throwing if the evaluation fails.
     *
     * @param message the original user message/question
     * @param answer the model-generated answer to validate
     * @param context supporting documents the answer should be consistent with
     * @return the answer, unchanged, if it passes evaluation
     * @throws com.multimodel.llm.exception.InvalidAnswerException if the answer fails the fact-checking evaluation
     */
    private String validateAnswer(String message,
                                  String answer,
                                  List<Document> context) {

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, context, answer);

        EvaluationResponse evaluationResponse =
                factCheckingEvaluator.evaluate(evaluationRequest);

        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
        return answer;
    }
}
