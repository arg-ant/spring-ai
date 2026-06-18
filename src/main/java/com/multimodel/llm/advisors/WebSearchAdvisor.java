package com.multimodel.llm.advisors;

import com.multimodel.llm.rag.FirecrawlWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.web.client.RestClient;

import static com.multimodel.llm.config.Constants.MAX_NUM_WEB_SEARCH_RESULTS;

public class WebSearchAdvisor {

    public static Advisor webSearchAdvisor(RestClient.Builder restClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(FirecrawlWebSearchDocumentRetriever.builder()
                        .restClientBuilder(restClientBuilder)
                        .maxResults(MAX_NUM_WEB_SEARCH_RESULTS)
                        .build())
                .build();
    }
}

