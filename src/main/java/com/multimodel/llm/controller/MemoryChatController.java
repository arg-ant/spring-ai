package com.multimodel.llm.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * REST controller exposing a chat endpoint backed by a {@link ChatClient} augmented with
 * conversation memory, so replies take prior messages in the same conversation into account.
 */
@RestController
@RequestMapping("/api")
public class MemoryChatController {

    private final ChatClient chatClient;

    /**
     * Creates a new controller backed by the given memory-augmented chat client.
     *
     * @param chatClient the chat client used to serve requests
     */
    public MemoryChatController(@Qualifier("memoryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Sends the given message to the model, scoping conversation memory to the supplied
     * conversation ID so prior messages in the same conversation are recalled.
     *
     * @param conversationId the conversation identifier, bound from the {@code username} request header
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
    @GetMapping("/chat-memory")
    public ResponseEntity<String> chatMemory(@RequestHeader("username") String conversationId,
                                             @RequestParam("message") String message) {
        return ResponseEntity.ok(chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        .param(CONVERSATION_ID, conversationId))
                .call()
                .content());
    }
}
