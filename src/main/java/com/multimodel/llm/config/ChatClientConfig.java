package com.multimodel.llm.config;


import com.multimodel.llm.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import static com.multimodel.llm.advisors.MemoryAdvisor.memoryAdvisor;
import static com.multimodel.llm.advisors.WebSearchAdvisor.webSearchAdvisor;
import static com.multimodel.llm.config.Constants.MAX_MESSAGES;

/**
 * Configuration class that assembles the various {@link ChatClient} beans used across the
 * application, each pre-wired with a different combination of model provider, advisors
 * (memory, RAG, web search), tools, and system prompts.
 */
@Configuration
public class ChatClientConfig {

    /**
     * System prompt template used by {@link #helpDeskChatClient}.
     */
    @Value("classpath:/promptTemplates/helpDeskSystemPromptTemplate.st")
    private Resource helpDeskSystemPromptTemplate;

    /**
     * A plain chat client backed by OpenAI, with no advisors, tools, or custom system prompt.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @return the configured chat client
     */
    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createOpenAi().build();
    }

    /**
     * A plain chat client backed by Ollama, with no advisors, tools, or custom system prompt.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @return the configured chat client
     */
    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createOllama().build();
    }

    /**
     * A chat client backed by the Bespoke-Minicheck model, used for fact-checking /
     * hallucination-detection tasks.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @return the configured chat client
     */
    @Bean("bespokeMinicheckChatClient")
    public ChatClient bespokeMinicheckChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createBespokeMinicheck().build();
    }

    /**
     * A {@link ChatMemory} backed by a JDBC-persisted repository, retaining up to
     * {@link Constants#MAX_MESSAGES} messages per conversation window.
     *
     * @param jdbcChatMemoryRepository the JDBC repository used to persist chat messages
     * @return the configured chat memory
     */
    @Bean("jdbcChatMemory")
    public ChatMemory messageWindowChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    /**
     * An Ollama-backed chat client augmented with conversation memory.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @param chatMemory the chat memory used to retain conversation history
     * @return the configured chat client
     */
    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(ChatClientFactory chatClientFactory,
                                       ChatMemory chatMemory) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory))
                .build();
    }

    /**
     * An Ollama-backed chat client augmented with both conversation memory and
     * retrieval-augmented generation (RAG).
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @param chatMemory the chat memory used to retain conversation history
     * @param ragAdvisor the advisor that performs retrieval augmentation
     * @return the configured chat client
     */
    @Bean("ragMemoryChatClient")
    public ChatClient ragMemoryChatClient(ChatClientFactory chatClientFactory,
                                       ChatMemory chatMemory,
                                       RetrievalAugmentationAdvisor ragAdvisor) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory), ragAdvisor)
                .build();
    }

    /**
     * An Ollama-backed chat client equipped with {@link TimeTools} so it can answer
     * time-related questions via tool calls.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @param timeTools the tool implementation exposing time-related functions
     * @return the configured chat client
     */
    @Bean("timeChatClient")
    public ChatClient timeChatClient(ChatClientFactory chatClientFactory,
                                     TimeTools timeTools) {
        return chatClientFactory
                .createOllama()
                .defaultTools(timeTools)
                .build();
    }

    /**
     * An Ollama-backed chat client configured as an HR help-desk assistant: it uses
     * conversation memory, the help-desk system prompt template, and {@link TimeTools}.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @param chatMemory the chat memory used to retain conversation history
     * @param timeTools the tool implementation exposing time-related functions
     * @return the configured chat client
     */
    @Bean("helpDeskChatClient")
    public ChatClient helpDeskChatClient(ChatClientFactory chatClientFactory,
                                         ChatMemory chatMemory,
                                         TimeTools timeTools) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory))
                .defaultSystem(helpDeskSystemPromptTemplate)
                .defaultTools(timeTools).build();
    }

    /**
     * An Ollama-backed chat client augmented with conversation memory and a web-search
     * advisor, allowing it to ground responses in live search results.
     *
     * @param chatClientFactory factory used to build provider-specific chat clients
     * @param chatMemory the chat memory used to retain conversation history
     * @param restClientBuilder builder used by the web search advisor to call the search API
     * @return the configured chat client
     */
    @Bean("webSearchChatClient")
    public ChatClient webSearchChatClient(ChatClientFactory chatClientFactory,
                                          ChatMemory chatMemory,
                                          RestClient.Builder restClientBuilder) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory), webSearchAdvisor(restClientBuilder))
                .build();
    }
}
