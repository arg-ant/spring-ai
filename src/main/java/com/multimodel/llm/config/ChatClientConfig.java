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

@Configuration
public class ChatClientConfig {

    @Value("classpath:/promptTemplates/helpDeskSystemPromptTemplate.st")
    private Resource helpDeskSystemPromptTemplate;

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createOpenAi().build();
    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createOllama().build();
    }

    @Bean("bespokeMinicheckChatClient")
    public ChatClient bespokeMinicheckChatClient(ChatClientFactory chatClientFactory) {
        return chatClientFactory.createBespokeMinicheck().build();
    }

    @Bean("jdbcChatMemory")
    public ChatMemory messageWindowChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }

    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(ChatClientFactory chatClientFactory,
                                       ChatMemory chatMemory) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory))
                .build();
    }

    @Bean("ragMemoryChatClient")
    public ChatClient ragMemoryChatClient(ChatClientFactory chatClientFactory,
                                       ChatMemory chatMemory,
                                       RetrievalAugmentationAdvisor ragAdvisor) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory), ragAdvisor)
                .build();
    }

    @Bean("timeChatClient")
    public ChatClient timeChatClient(ChatClientFactory chatClientFactory,
                                     TimeTools timeTools) {
        return chatClientFactory
                .createOllama()
                .defaultTools(timeTools)
                .build();
    }

    @Bean("helpDeskChatClient")
    public ChatClient helpDeskChatClient(ChatClientFactory chatClientFactory,
                                         ChatMemory chatMemory,
                                         TimeTools timeTools) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory))
                .defaultSystem(helpDeskSystemPromptTemplate)
                .defaultTools(timeTools).build();
    }

    @Bean("webSearchChatClient")
    public ChatClient webSearchChatClient(ChatClientFactory chatClientFactory,
                                          ChatMemory chatMemory,
                                          RestClient.Builder restClientBuilder) {
        return chatClientFactory
                .createOllama(memoryAdvisor(chatMemory), webSearchAdvisor(restClientBuilder))
                .build();
    }
}
