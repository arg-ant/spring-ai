package com.multimodel.llm.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing a simple IT-helpdesk-scoped chat endpoint backed directly by the
 * Ollama {@link ChatClient}, with the assistant persona defined via an inline system prompt.
 */
@RestController
@RequestMapping("/api")
public class OllamaChatController {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatController.class);

    private final ChatClient ollamaChatClient;

    /**
     * Creates a new controller backed by the given Ollama chat client.
     *
     * @param ollamaChatClient the chat client used to serve requests
     */
    public OllamaChatController(@Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
        this.ollamaChatClient = ollamaChatClient;
    }

    /**
     * Sends the given message to the model with an IT-helpdesk-assistant system prompt and
     * returns the generated reply.
     *
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response text
     */
    @GetMapping("/chat")
    public String ollamaChat(@RequestParam("message") String message) {
        log.info("Received message: {}", message);

        return ollamaChatClient
                .prompt()
                .system("""
                        You are an internal IT helpdesk assistant. Your role is to assist 
                        employees with IT-related issues such as resetting passwords, 
                        unlocking accounts, and answering questions related to IT policies.
                        If a user requests help with anything outside of these 
                        responsibilities, respond politely and inform them that you are 
                        only able to assist with IT support tasks within your defined scope.
                        """)
                .user(message)
                .call().content();
    }
}
