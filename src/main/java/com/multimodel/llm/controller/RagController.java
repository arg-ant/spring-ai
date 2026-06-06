package com.multimodel.llm.controller;

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

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final ChatClient chatMemoryClient;
    private final ChatClient webSearchChatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/promptTemplates/systemPromptRandomDataTemplate.st")
    Resource promptTemplate;

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource hrSystemTemplate;

    public RagController(@Qualifier("chatMemoryClient") ChatClient chatMemoryClient,
                         @Qualifier("webSearchRagChatClient")ChatClient webSearchChatClient,
                         VectorStore vectorStore) {
        this.chatMemoryClient = chatMemoryClient;
        this.webSearchChatClient = webSearchChatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/random/chat")
    public ResponseEntity<String> randomChat(
            @RequestHeader("username") String username,
            @RequestParam("message") String message) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)                  // Convert the user's question into an embedding and use it for vector search
                .topK(3)                         // Return at most 3 most similar documents
                .similarityThreshold(0.5)        // Ignore documents with similarity below 50%
                .build();

        List<Document> similarDocs =
                vectorStore.similaritySearch(searchRequest); // Search Qdrant for matching documents

        String similarContext = similarDocs.stream()
                .map(Document::getText)                     // Extract text from each retrieved document
                .collect(Collectors.joining(System.lineSeparator())); // Merge all document texts into a single context string

        String answer = chatMemoryClient.prompt()

                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)               // Load the system prompt template
                        .param("documents", similarContext)) // Replace {documents} placeholder with retrieved context

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

        // below code not needed as it is done by RetrievalAugmentationAdvisor coming from ollamaMemoryChatClient()
        //        SearchRequest searchRequest =
//                SearchRequest.builder().query(message).topK(3).similarityThreshold(0.5).build();
//        List<Document> similarDocs =  vectorStore.similaritySearch(searchRequest);
//        String similarContext = similarDocs.stream()
//                .map(Document::getText)
//                .collect(Collectors.joining(System.lineSeparator()));
        String answer = chatMemoryClient.prompt()
//                .system(promptSystemSpec -> promptSystemSpec.text(hrSystemTemplate)
//                        .param("documents", similarContext))
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/web-search/chat")
    public ResponseEntity<String> webSearchChat(@RequestHeader("username") String username,
                                               @RequestParam("message") String message) {
        String answer = webSearchChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

}
