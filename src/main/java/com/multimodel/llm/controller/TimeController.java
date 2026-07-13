package com.multimodel.llm.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * REST controller exposing a chat endpoint backed by a {@link ChatClient} equipped with
 * time-related tools, allowing the model to answer questions about the current local time.
 */
@RestController
@RequestMapping("/api/tools")
public class TimeController {

    private final ChatClient chatClient;

    /**
     * Creates a new controller backed by the given time-aware chat client.
     *
     * @param chatClient the chat client used to serve requests
     */
    public TimeController(@Qualifier("timeChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Sends the given message to the model, scoping conversation memory to the supplied
     * conversation ID so the model can use its time tools within that conversation's context.
     *
     * @param conversationId the conversation identifier, bound from the {@code username} request header
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
    @GetMapping("/local-time")
    public ResponseEntity<String> localTime(@RequestHeader("username") String conversationId,
                                            @RequestParam("message") String message) {
        String answer = chatClient
                .prompt()
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
        return ResponseEntity.ok(answer);
    }
}
