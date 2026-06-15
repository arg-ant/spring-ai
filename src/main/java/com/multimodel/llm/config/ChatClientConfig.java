package com.multimodel.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ChatClientConfig {

    @Bean("defaultChatClient")
    @Scope("prototype")
    public ChatClient.Builder chatClientBuilder(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel);
    }

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(ChatClientFactory factory) {
        return factory.createOpenAi().build();
    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatClientFactory factory) {
        return factory.createOllama().build();
    }

    @Bean("bespokeMinicheckChatClient")
    public ChatClient bespokeMinicheckChatClient(ChatClientFactory factory) {
        return factory.createBespokeMinicheck().build();
    }

    @Bean("jdbcChatMemory")
    public ChatMemory chatMemoryWindowClient(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    @Bean("chatMemoryClient")
    public ChatClient memoryChatClient(ChatClient.Builder chatClientBuilder,
                                       ChatMemory chatMemory,
                                       RetrievalAugmentationAdvisor ragAdvisor) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return chatClientBuilder
                .defaultAdvisors(
                        memoryAdvisor,
                        ragAdvisor,
                        loggerAdvisor)
                .build();
    }
}
