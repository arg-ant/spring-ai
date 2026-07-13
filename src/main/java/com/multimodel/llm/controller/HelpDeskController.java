package com.multimodel.llm.controller;


import com.multimodel.llm.tools.HelpDeskTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * REST controller exposing an HR help-desk chat endpoint backed by a {@link ChatClient}
 * equipped with {@link HelpDeskTools}, letting the model perform help-desk actions (e.g.
 * lookups) scoped to the requesting user.
 */
@RestController
@RequestMapping("/api/tools")
public class HelpDeskController {

    private final ChatClient chatClient;
    private final HelpDeskTools helpDeskTools;

    /**
     * Creates a new controller backed by the given help-desk chat client and tools.
     *
     * @param chatClient the chat client used to serve requests
     * @param helpDeskTools the tool implementation exposing help-desk functions
     */
    public HelpDeskController(@Qualifier("helpDeskChatClient") ChatClient chatClient,
            HelpDeskTools helpDeskTools) {
        this.chatClient = chatClient;
        this.helpDeskTools = helpDeskTools;
    }

    /**
     * Sends the given message to the model with conversation memory and help-desk tools
     * enabled, passing the requesting username into the tool context so tools can act on
     * behalf of that user.
     *
     * @param username the conversation identifier and tool-context username, bound from the
     *                 {@code username} request header
     * @param message the user's message, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
    @GetMapping("/help-desk")
    public ResponseEntity<String> helpDesk(@RequestHeader("username") String username,
            @RequestParam("message") String message) {
        String answer = chatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .tools(helpDeskTools)
                .toolContext(Map.of("username", username))
                .call().content();
        return ResponseEntity.ok(answer);
    }
}
