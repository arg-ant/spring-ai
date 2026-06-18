package com.multimodel.llm.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.multimodel.llm.config.Constants.*;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatClient ragChatMemoryClient;
    private final ChatClient webSearchChatClient;
    private final VectorStore vectorStore;


    public RagController(@Qualifier("ragMemoryChatClient") ChatClient ragChatMemoryClient,
                         @Qualifier("webSearchChatClient")ChatClient webSearchChatClient,
                         VectorStore vectorStore) {
        this.ragChatMemoryClient = ragChatMemoryClient;
        this.webSearchChatClient = webSearchChatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/random/chat")
    public ResponseEntity<@NonNull String> randomChat(
            @RequestHeader("username") String username,
            @RequestParam("message") String message) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)                  // Convert the user's question into an embedding and use it for vector search
                .topK(TOP_K)                         // Return at most 3 most similar documents
                .similarityThreshold(SIMILARITY_THRESHOLD)        // Ignore documents with similarity below 50%
                .build();

        List<Document> similarDocs =
                vectorStore.similaritySearch(searchRequest); // Search Qdrant for matching documents

        String similarContext = similarDocs.stream()
                .map(Document::getText)                     // Extract text from each retrieved document
                .collect(Collectors.joining(System.lineSeparator())); // Merge all document texts into a single context string

        String answer = ragChatMemoryClient.prompt()

                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptTemplate)               // Load the system prompt template
                        .param(DOCUMENTS_PLACEHOLDER, similarContext)) // Replace {documents} placeholder with retrieved context

                .advisors(a -> a.param(CONVERSATION_ID, username))
                // Use username as conversation ID so previous messages can be remembered

                .user(message)                             // Add the user's question to the prompt

                .call()                                    // Send prompt to the LLM
                .content();                                // Extract generated response text

        return ResponseEntity.ok(answer);                  // Return answer as HTTP 200 response
    }



    @GetMapping("/document/chat")
    public ResponseEntity<String> documentChat(@RequestHeader("username") String username,
                                             @RequestParam("message") String message) {
        String answer = ragChatMemoryClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/web-search/chat")
    public ResponseEntity<@NonNull String> webSearchChat(@RequestHeader("username") String username,
                                               @RequestParam("message") String message) {
        String answer = webSearchChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

}
