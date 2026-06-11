package com.multimodel.llm.config;

import com.multimodel.llm.advisors.TokenUsageLoggerAdvisor;
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
public class ChatClientConfig {

    @Bean("defaultChatClient")
    @Scope("prototype")
    public ChatClient.Builder chatClientBuilder(
            OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel);
    }


    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
//        var options = OllamaChatOptions.builder().temperature(0.7).maxTokens(250);

//        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel);
        ChatClient.Builder builder = chatClientBuilder(ollamaChatModel);

        return builder
//                .defaultSystem("You are an internal IT helpdesk assistant.")
//                .defaultUser("What can I help you with?")
//                .defaultOptions(new OllamaChatOptions.Builder()
//                        .temperature(0.7)
//                        .maxTokens(200))
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),  //log all messages
                        new TokenUsageLoggerAdvisor())
                .build();
    }

    @Bean("jdbcChatMemory")
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder().maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository).build();
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
