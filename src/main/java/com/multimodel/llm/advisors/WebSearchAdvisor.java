package com.multimodel.llm.advisors;

import com.multimodel.llm.rag.FirecrawlWebSearchDocumentRetriever;
import com.multimodel.llm.rag.TavilyWebSearchDocumentRetriever;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.web.client.RestClient;

import static com.multimodel.llm.config.Constants.MAX_NUM_WEB_SEARCH_RESULTS;

/**
 * Factory for an {@link Advisor} that augments chat requests with live web
 * search results retrieved via {@link FirecrawlWebSearchDocumentRetriever}.
 */
public class WebSearchAdvisor {

    /**
     * Creates an advisor that retrieves web search results for the current query
     * and injects them as context, using
     * {@link com.multimodel.llm.config.Constants#MAX_NUM_WEB_SEARCH_RESULTS} as the result limit.
     *
     * @param restClientBuilder the REST client builder used to call the Firecrawl API
     * @return an {@link Advisor} backed by a {@link RetrievalAugmentationAdvisor}
     */
    public static Advisor webSearchAdvisor(RestClient.Builder restClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
//                        FirecrawlWebSearchDocumentRetriever
                        TavilyWebSearchDocumentRetriever
                                .builder()
                        .restClientBuilder(restClientBuilder)
                        .maxResults(MAX_NUM_WEB_SEARCH_RESULTS)
                        .build())
                .build();
    }
}

