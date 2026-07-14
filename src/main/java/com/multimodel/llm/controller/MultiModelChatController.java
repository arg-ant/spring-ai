package com.multimodel.llm.controller;

import com.multimodel.llm.model.CountryCities;
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

import static com.multimodel.llm.config.Constants.CUSTOMER_MESSAGE_PLACEHOLDER;
import static com.multimodel.llm.config.Constants.CUSTOMER_NAME_PLACEHOLDER;

/**
 * REST controller demonstrating a range of Spring AI {@link ChatClient} capabilities across
 * two model providers (OpenAI and Ollama): plain chat, prompt templating, prompt stuffing,
 * streaming responses, and structured output conversion (bean, list, map, and bean-list).
 */
@RestController
@RequestMapping("/api")
public class MultiModelChatController {

    /**
     * Template used to render customer-support emails in {@link #email}.
     */
    @Value("classpath:/promptTemplates/userPromptTemplate.st")
    Resource userPromptTemplate;

    /**
     * HR policy template used as a system prompt in {@link #promptStuff}.
     */
    @Value("classpath:/promptTemplates/hrPolicyTemplate.st")
    private Resource hrPolicyTemplate;

    /**
     * General-purpose system prompt template (unused directly in this controller's endpoints).
     */
    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatClient openAiChatClient;
    private final ChatClient ollamaChatClient;


    /**
     * Creates a new controller backed by the given OpenAI and Ollama chat clients.
     *
     * @param openAiChatClient chat client backed by OpenAI
     * @param ollamaChatClient chat client backed by Ollama
     */
    public MultiModelChatController(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
        this.openAiChatClient = openAiChatClient;
        this.ollamaChatClient = ollamaChatClient;
    }

    /**
     * Sends the given message to the OpenAI-backed chat client.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response text
     */
    @RequestMapping("/openai/chat")
    public String openAiChat(@RequestParam("message") String message) {
        return openAiChatClient.prompt(message).call().content();
    }

    /**
     * Sends the given message to the Ollama-backed chat client.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response text
     */
    @RequestMapping("/ollama/chat")
    public String chat(@RequestParam("message") String message) {
        return ollamaChatClient.prompt(message).call().content();
    }

    /**
     * Generates a customer-support email response by rendering the user prompt template with
     * the given customer name and message.
     *
     * @param customerName the customer's name, bound from the {@code customerName} query parameter
     * @param customerMessage the customer's message, bound from the {@code customerMessage} query parameter
     * @return the generated email content
     */
    @RequestMapping("/email")
    public String email(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerMessage") String customerMessage) {

        return ollamaChatClient
                .prompt(customerName)
                .system(userPromptTemplate)
                .user(promptTemplateSpec ->
                        promptTemplateSpec.text(userPromptTemplate)
                                .param(CUSTOMER_NAME_PLACEHOLDER, customerName)
                                .param(CUSTOMER_MESSAGE_PLACEHOLDER, customerMessage))
                .call()
                .content();
    }

    /**
     * Answers the given message with the HR policy document "stuffed" into the system prompt
     * as context.
     *
     * @param message the user's question, bound from the {@code message} query parameter
     * @return the model's response text
     */
    @RequestMapping("/prompt-stuffing")
    public String promptStuff(@RequestParam("message") String message) {
        return ollamaChatClient
                .prompt(message)
                .system(hrPolicyTemplate)
                .user(message)
                .call()
                .content();
    }

    /**
     * Streams the model's response to the given message as it is generated.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return a stream of response text chunks
     */
    @RequestMapping("/stream")
    public Flux<String> stream(@RequestParam("message") String message) {
        return ollamaChatClient
                .prompt()
                .user(message)
                .stream()
                .content();
    }

    /**
     * Sends the given message to the model and converts the response into a
     * {@link CountryCities} bean.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the structured response wrapped in a 200 OK response
     */
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

    /**
     * Sends the given message to the model and converts the response into a list of strings.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the structured response wrapped in a 200 OK response
     */
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

    /**
     * Sends the given message to the model and converts the response into a map.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the structured response wrapped in a 200 OK response
     */
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

    /**
     * Sends the given message to the model and converts the response into a list of
     * {@link CountryCities} beans.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the structured response wrapped in a 200 OK response
     */
    @RequestMapping("/chat-bean-list")
    public ResponseEntity<List<CountryCities>> chatBeanList(@RequestParam("message") String message) {
        List<CountryCities> countryCities =
                ollamaChatClient
                        .prompt()
                        .user(message)
                        .call()
                        .entity(new ParameterizedTypeReference<>() {
                        });

        return ResponseEntity.ok(countryCities);
    }
}
