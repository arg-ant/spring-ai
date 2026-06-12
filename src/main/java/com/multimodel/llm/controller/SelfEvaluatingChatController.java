package com.multimodel.llm.controller;

import com.multimodel.llm.exception.InvalidAnswerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/evaluate")
public class SelfEvaluatingChatController {

    private final ChatClient ollamaChatClient;
    private final FactCheckingEvaluator factCheckingEvaluator;


    //    hrPolicy used only in tests
    @Value("classpath:/promptTemplates/hrPolicy.st")
    Resource hrPolicyTemplate;

    public SelfEvaluatingChatController(
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
        this.ollamaChatClient = ollamaChatClient;
        ChatClient.Builder chatClientBuilder = this.ollamaChatClient.mutate();
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();
    }

    @RequestMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        String aiResponse = ollamaChatClient.prompt(message)
                .call().content();
        validateAnswer(message, aiResponse);
        return aiResponse;
    }

    @RequestMapping("/prompt-stuffing")
    public String promptStuff(@RequestParam("message") String message) {
        return ollamaChatClient
                .prompt(message)
                .system(hrPolicyTemplate) // hrPolicy used only in tests
                .user(message)
                .call()
                .content();
    }

    private void validateAnswer(String message, String answer) {
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, List.of(), answer);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }

}
