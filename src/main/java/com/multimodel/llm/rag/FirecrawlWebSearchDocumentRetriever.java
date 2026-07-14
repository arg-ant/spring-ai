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

/**
 * {@link DocumentRetriever} that performs a live web search via the
 * <a href="https://www.firecrawl.dev/">Firecrawl</a> {@code /search} API and
 * converts the results into {@link Document}s for use in retrieval-augmented
 * generation.
 * <p>
 * Requires the {@code FIRECRAWL_API_KEY} environment variable to be set.
 */
public class FirecrawlWebSearchDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlWebSearchDocumentRetriever.class);

    private static final String FIRECRAWL_API_KEY = "FIRECRAWL_API_KEY";
    private static final String FIRECRAWL_BASE_URL = "https://api.firecrawl.dev/v1";
    private static final int DEFAULT_RESULT_LIMIT = 3;
    private final int resultLimit;
    private final RestClient restClient;

    /**
     * Creates a retriever backed by a Firecrawl-configured {@link RestClient}.
     *
     * @param clientBuilder the REST client builder to configure with the Firecrawl
     *                       base URL and bearer token
     * @param resultLimit   maximum number of search results to request, must be greater than 0
     * @throws IllegalArgumentException if {@code clientBuilder} is null, the
     *                                   {@code FIRECRAWL_API_KEY} environment variable is unset, or
     *                                   {@code resultLimit} is not positive
     */
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

    /**
     * Executes the query against Firecrawl's web search endpoint and maps each
     * search hit to a {@link Document}, preferring the crawled markdown content
     * and falling back to the search result description.
     *
     * @param query the query to search for
     * @return the matching documents, or an empty list if the search returned no results
     */
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
        //#TODO Check why response.data() is []
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

    /** Request body sent to the Firecrawl {@code /search} endpoint. */
    record FirecrawlRequestPayload(String query, int limit) {
    }

    /** Response body returned by the Firecrawl {@code /search} endpoint. */
    record FirecrawlResponsePayload(Boolean success, List<Hit> data) {
        /** A single search result. */
        record Hit(String title, String url, String description, String markdown) {
        }
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link FirecrawlWebSearchDocumentRetriever}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link FirecrawlWebSearchDocumentRetriever}. */
    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int resultLimit = DEFAULT_RESULT_LIMIT;

        private Builder() {
        }

        /**
         * Sets the REST client builder used to call the Firecrawl API.
         *
         * @param clientBuilder the REST client builder
         * @return this builder
         */
        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        /**
         * Sets the maximum number of search results to request from Firecrawl.
         * Defaults to {@value #DEFAULT_RESULT_LIMIT}.
         *
         * @param maxResults maximum number of results, must be greater than 0
         * @return this builder
         * @throws IllegalArgumentException if {@code maxResults} is not positive
         */
        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.resultLimit = maxResults;
            return this;
        }

        /**
         * Builds the configured {@link FirecrawlWebSearchDocumentRetriever}.
         *
         * @return a new retriever instance
         */
        public FirecrawlWebSearchDocumentRetriever build() {
            return new FirecrawlWebSearchDocumentRetriever(clientBuilder, resultLimit);
        }
    }
}
