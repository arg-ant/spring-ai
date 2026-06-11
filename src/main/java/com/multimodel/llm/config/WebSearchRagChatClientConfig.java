package com.multimodel.llm.config;

import com.multimodel.llm.advisors.TokenUsageLoggerAdvisor;
import com.multimodel.llm.rag.FirecrawlWebSearchDocumentRetriever;
import com.multimodel.llm.rag.TavilyWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebSearchRagChatClientConfig {

    @Bean("webSearchRagChatClient")
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ChatMemory chatMemory,
                                 RestClient.Builder restClientBuilder) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        var webSearchRAGAdvisor = RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(TavilyWebSearchDocumentRetriever.builder() //Tavily search engine
                .documentRetriever(FirecrawlWebSearchDocumentRetriever.builder() //Firecrawl search engine
                        .restClientBuilder(restClientBuilder).maxResults(5).build())
                .build();

        return chatClientBuilder
                .defaultAdvisors(
                        memoryAdvisor,
                        webSearchRAGAdvisor,
                        new TokenUsageLoggerAdvisor(),
                        loggerAdvisor)
                .build();
    }
}
