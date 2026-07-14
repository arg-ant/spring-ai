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

/**
 * REST controller exposing retrieval-augmented generation (RAG) chat endpoints: one that
 * manually retrieves context from the vector store and stuffs it into the prompt, one that
 * relies on a RAG advisor to do so automatically, and one that grounds responses in live
 * web search results.
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    /**
     * System prompt template used by {@link #randomChat} to inject retrieved document context.
     */
    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    private Resource systemPromptTemplate;

    private final ChatClient ragChatMemoryClient;
    private final ChatClient webSearchChatClient;
    private final VectorStore vectorStore;


    /**
     * Creates a new controller backed by the given chat clients and vector store.
     *
     * @param ragChatMemoryClient chat client augmented with conversation memory and RAG, used
     *                             for document-based chat
     * @param webSearchChatClient chat client augmented with conversation memory and web search
     * @param vectorStore vector store queried directly for manual similarity search
     */
    public RagController(@Qualifier("ragMemoryChatClient") ChatClient ragChatMemoryClient,
                         @Qualifier("webSearchChatClient")ChatClient webSearchChatClient,
                         VectorStore vectorStore) {
        this.ragChatMemoryClient = ragChatMemoryClient;
        this.webSearchChatClient = webSearchChatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Answers the given message by manually retrieving the most similar documents from the
     * vector store, injecting their text into the system prompt template as context, and
     * calling the model. Conversation memory is scoped to the given username.
     * <p>
     * Requires the vector store to have been populated with sample data beforehand. See
     * {@link com.multimodel.llm.rag.RandomDataLoader}, whose {@code @Component} annotation
     * must be uncommented so it loads its sentences into the vector store on startup.
     *
     * @param username the conversation identifier, bound from the {@code username} request header
     * @param message the user's question, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
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



    /**
     * Answers the given message using the RAG-augmented chat client, which retrieves relevant
     * document context automatically via its configured RAG advisor. Conversation memory is
     * scoped to the given username.
     * <p>
     * Requires the vector store to have been populated with the HR policies document
     * beforehand. See {@link com.multimodel.llm.rag.HRPolicyLoader}, whose {@code @Component}
     * annotation must be uncommented so it loads the document into the vector store on startup.
     *
     * @param username the conversation identifier, bound from the {@code username} request header
     * @param message the user's question, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
    @GetMapping("/document/chat")
    public ResponseEntity<String> documentChat(@RequestHeader("username") String username,
                                             @RequestParam("message") String message) {
        String answer = ragChatMemoryClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

    /**
     * Answers the given message using the web-search-augmented chat client, grounding the
     * response in live search results. Conversation memory is scoped to the given username.
     *
     * @param username the conversation identifier, bound from the {@code username} request header
     * @param message the user's question, bound from the {@code message} query parameter
     * @return the model's response wrapped in a 200 OK response
     */
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
