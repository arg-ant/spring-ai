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


@RestController
@RequestMapping("/api/evaluate")
public class SelfEvaluatingChatController {

    private final ChatClient ollamaChatClient;
    private final FactCheckingEvaluator factCheckingEvaluator;

    @Value("classpath:/promptTemplates/hrPolicyTemplate.st")
    Resource hrPolicyTemplate;

    public SelfEvaluatingChatController(
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
            @Qualifier("bespokeMinicheckChatClient") ChatClient bespokeMinicheckChatClient) {
        this.ollamaChatClient = ollamaChatClient;
        this.factCheckingEvaluator = FactCheckingEvaluator
                .builder(bespokeMinicheckChatClient.mutate()).build();
    }

    @RequestMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        String aiResponse = ollamaChatClient.prompt(message)
                .call().content();
        validateAnswer(message, aiResponse, List.of());
        return aiResponse;
    }

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
        return aiResponse;
    }

    private void validateAnswer(String message,
                                String answer,
                                List<Document> context) {

        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, context, answer);

        EvaluationResponse evaluationResponse =
                factCheckingEvaluator.evaluate(evaluationRequest);

        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }
}
