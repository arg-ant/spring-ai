package com.multimodel.llm.controller;

import com.multimodel.llm.model.CountryCities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MultiModelChatController {

    private static final Logger log = LoggerFactory.getLogger(MultiModelChatController.class);

    private final ChatClient openAiChatClient;
    private final ChatClient ollamaChatClient;

    public MultiModelChatController(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.ollamaChatClient = ollamaChatClient;
    }

    @RequestMapping("/openai/chat")
    public String openAiChat(@RequestParam("message") String message) {
        logMessage(message);
        return openAiChatClient.prompt(message).call().content();
    }

    @RequestMapping("/ollama/chat")
    public String chat(@RequestParam("message") String message) {
        logMessage(message);
        return ollamaChatClient.prompt(message).call().content();
    }

    @Value("classpath:/prompttemplates/userPromptTemplate.st")
    Resource promptTemplate;

    //custom prompt with parameters for Ollama model
    @RequestMapping("/email")
    public String ollamaEmail(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerMessage") String customerMessage) {

        logMessage(customerName, customerMessage);
        return ollamaChatClient
                .prompt(customerName)
                .system(promptTemplate)
                .user(promptTemplateSpec ->
                        promptTemplateSpec.text(promptTemplate)
                                .param("customerName", customerName)
                                .param("customerMessage", customerMessage))
                .call()
                .content();
    }

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource systemPromptTemplate;

//    hrPolicy used only in tests
    @Value("classpath:/promptTemplates/hrPolicy.st")
    Resource hrPolicyTemplate;

    @RequestMapping("/prompt-stuffing")
    public String promptStuff(@RequestParam("message") String message) {
        logMessage(message);
        return ollamaChatClient
                .prompt(message)
                .system(hrPolicyTemplate) // hrPolicy used only in tests
//                .system(systemPromptTemplate) //system prompt used in development
                .user(message)
                .call()
                .content();
    }

    @RequestMapping("/stream")
    public Flux<String> ollamaStream(@RequestParam("message") String message) {
        logMessage(message);
        return ollamaChatClient
                .prompt()
                .user(message)
                .stream()
                .content();
    }

    @RequestMapping("/chat-bean")
    public ResponseEntity<CountryCities> chatBean(@RequestParam("message") String message) {
        CountryCities countryCities =
                ollamaChatClient
                .prompt()
                .user(message)
                .call()
                .entity(CountryCities.class);

        return ResponseEntity.ok(countryCities);
    }

    @RequestMapping("/chat-list")
    public ResponseEntity<List<String>> chatList(@RequestParam("message") String message) {
        List<String> countryCities =
                ollamaChatClient
                        .prompt()
                        .user(message)
                        .call()
                        .entity(new ListOutputConverter());

        return ResponseEntity.ok(countryCities);
    }

    @RequestMapping("/chat-map")
    public ResponseEntity<Map<String, Object>> chatMap(@RequestParam("message") String message) {
        Map<String, Object> countryCities =
                ollamaChatClient
                        .prompt()
                        .user(message)
                        .call()
                        .entity(new MapOutputConverter());

        return ResponseEntity.ok(countryCities);
    }

    @RequestMapping("/chat-bean-list")
    public ResponseEntity<List<CountryCities>> chatBeanList(@RequestParam("message") String message) {
        List<CountryCities> countryCities =
                ollamaChatClient
                        .prompt()
                        .user(message)
                        .call()
                        .entity(new ParameterizedTypeReference<List<CountryCities>>() {
                        });

        return ResponseEntity.ok(countryCities);
    }

    private void logMessage(String... messages) {
        String joinedMessage = String.join(" | ", messages);
        log.info("Received message(s): {}", joinedMessage);
    }
}
