package com.multimodel.llm.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

public class FirecrawlWebSearchDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlWebSearchDocumentRetriever.class);

    private static final String FIRECRAWL_API_KEY = "FIRECRAWL_API_KEY";
    private static final String FIRECRAWL_BASE_URL = "https://api.firecrawl.dev/v1";
    private static final int DEFAULT_RESULT_LIMIT = 3;
    private final int resultLimit;
    private final RestClient restClient;

    public FirecrawlWebSearchDocumentRetriever(RestClient.Builder clientBuilder, int resultLimit) {
        Assert.notNull(clientBuilder, "clientBuilder cannot be null");
        String apiKey = System.getenv(FIRECRAWL_API_KEY);
        Assert.hasText(apiKey, "Environment variable " + FIRECRAWL_API_KEY + " must be set");
        this.restClient = clientBuilder
                .baseUrl(FIRECRAWL_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        if (resultLimit <= 0) {
            throw new IllegalArgumentException("resultLimit must be greater than 0");
        }
        this.resultLimit = resultLimit;
    }

    @Override
    public List<Document> retrieve(Query query) {
        logger.info("Processing query: {}", query.text());
        Assert.notNull(query, "query cannot be null");

        String q = query.text();
        Assert.hasText(q, "query.text() cannot be empty");

        FirecrawlResponsePayload response = restClient.post()
                .uri("/search")
                .body(new FirecrawlRequestPayload(q, resultLimit))
                .retrieve()
                .body(FirecrawlResponsePayload.class);

        if (response == null || CollectionUtils.isEmpty(response.data())) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>(response.data().size());
        for (FirecrawlResponsePayload.Hit hit : response.data()) {
            String content = hit.markdown() != null ? hit.markdown() : hit.description();
            Document doc = Document.builder()
                    .text(content)
                    .metadata("title", hit.title())
                    .metadata("url", hit.url())
                    .build();
            logger.info("Web search result [title={}, url={}]: {}", hit.title(), hit.url(), content);
            docs.add(doc);
        }
        return docs;
    }

    record FirecrawlRequestPayload(String query, int limit) {
    }

    record FirecrawlResponsePayload(Boolean success, List<Hit> data) {
        record Hit(String title, String url, String description, String markdown) {
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int resultLimit = DEFAULT_RESULT_LIMIT;

        private Builder() {
        }

        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.resultLimit = maxResults;
            return this;
        }

        public FirecrawlWebSearchDocumentRetriever build() {
            return new FirecrawlWebSearchDocumentRetriever(clientBuilder, resultLimit);
        }
    }
}
