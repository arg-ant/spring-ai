package com.multimodel.llm.config;

import com.multimodel.llm.advisors.TokenUsageLoggerAdvisor;
import com.multimodel.llm.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TimeChatClientConfig {

    @Bean("timeChatClient")
    public ChatClient timeChatClient(ChatClient.Builder chatClientBuilder,
                                       ChatMemory chatMemory,
                                     TimeTools timeTools
                                       ) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return chatClientBuilder
                .defaultTools(timeTools)
                .defaultAdvisors(
                        memoryAdvisor,
                        loggerAdvisor)
                .build();
    }
}
