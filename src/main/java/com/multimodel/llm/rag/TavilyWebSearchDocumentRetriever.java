package com.multimodel.llm.rag;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
 * A {@link DocumentRetriever} that queries the <a href="https://tavily.com">Tavily</a> web
 * search API and maps the returned hits into Spring AI {@link Document}s, for use as a
 * web-search-backed RAG retrieval source.
 * <p>
 * Requires the {@code TAVILY_SEARCH_API_KEY} environment variable to be set. Instances are
 * created via {@link #builder()}.
 */
public class TavilyWebSearchDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(TavilyWebSearchDocumentRetriever.class);

    /** Name of the environment variable holding the Tavily API key. */
    private static final String TAVILY_API_KEY = "TAVILY_SEARCH_API_KEY";
    /** Tavily search API endpoint. */
    private static final String TAVILY_BASE_URL = "https://api.tavily.com/search";
    /** Default number of search results to request when none is specified. */
    private static final int DEFAULT_RESULT_LIMIT = 3;
    private final int resultLimit;
    private final RestClient restClient;

    /**
     * Creates a new retriever configured to call the Tavily search API.
     *
     * @param clientBuilder builder used to construct the underlying {@link RestClient},
     *                       pre-configured with the Tavily base URL and bearer auth header
     * @param resultLimit maximum number of search results to request per query; must be greater than 0
     * @throws IllegalArgumentException if {@code clientBuilder} is null, the
     *         {@code TAVILY_SEARCH_API_KEY} environment variable is not set, or
     *         {@code resultLimit} is not greater than 0
     */
    public TavilyWebSearchDocumentRetriever(RestClient.Builder clientBuilder, int resultLimit) {
        Assert.notNull(clientBuilder, "clientBuilder cannot be null");
        String apiKey = System.getenv(TAVILY_API_KEY);
        Assert.hasText(apiKey, "Environment variable " + TAVILY_API_KEY + " must be set");
        this.restClient = clientBuilder
                .baseUrl(TAVILY_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        if (resultLimit <= 0) {
            throw new IllegalArgumentException("resultLimit must be greater than 0");
        }
        this.resultLimit = resultLimit;
    }

    /**
     * Executes the given query against the Tavily search API and maps the results into
     * Spring AI documents.
     *
     * @param query the query to search for; its text must be non-null and non-empty
     * @return a list of documents built from the search hits (text, title, url metadata, and
     *         relevance score), or an empty list if the API returns no results
     * @throws IllegalArgumentException if {@code query} is null or its text is empty
     */
    @Override
    public List<Document> retrieve(Query query) {
        logger.info("Processing query: {}", query.text());
        Assert.notNull(query, "query cannot be null");

        String q = query.text();
        Assert.hasText(q, "query.text() cannot be empty");

        TavilyResponsePayload response = restClient.post()
                .body(new TavilyRequestPayload(q, "advanced", resultLimit))
                .retrieve()
                .body(TavilyResponsePayload.class);

        if (response == null || CollectionUtils.isEmpty(response.results())) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>(response.results().size());
        for (TavilyResponsePayload.Hit hit : response.results()) {
            // Map each Tavily hit into a Spring AI Document with metadata and score.
            Document doc = Document.builder()
                    .text(hit.content())
                    .metadata("title", hit.title())
                    .metadata("url", hit.url())
                    .score(hit.score())
                    .build();
            logger.info("Web search result [title={}, url={}, score={}]: {}",
                    hit.title(), hit.url(), hit.score(), doc.getText());
            docs.add(doc);
        }
        return docs;
    }

    /**
     * Request body sent to the Tavily search API, serialized with snake_case field names.
     *
     * @param query the search query text
     * @param searchDepth Tavily search depth, e.g. {@code "advanced"}
     * @param maxResults maximum number of results to request
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TavilyRequestPayload(String query, String searchDepth, int maxResults) {
    }

    /**
     * Response body returned by the Tavily search API.
     *
     * @param results the list of search hits
     */
    record TavilyResponsePayload(List<Hit> results) {
        /**
         * A single Tavily search result.
         *
         * @param title the page title
         * @param url the page URL
         * @param content the extracted page content
         * @param score the relevance score assigned by Tavily
         */
        record Hit(String title, String url, String content, Double score) {
        }
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link TavilyWebSearchDocumentRetriever}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TavilyWebSearchDocumentRetriever}.
     */
    public static class Builder {
        private RestClient.Builder clientBuilder;
        private int resultLimit = DEFAULT_RESULT_LIMIT;

        private Builder() {
        }

        /**
         * Sets the {@link RestClient.Builder} used to build the underlying HTTP client.
         *
         * @param clientBuilder the REST client builder
         * @return this builder
         */
        public Builder restClientBuilder(RestClient.Builder clientBuilder) {
            this.clientBuilder = clientBuilder;
            return this;
        }

        /**
         * Sets the maximum number of search results to request per query.
         *
         * @param maxResults maximum number of results; must be greater than 0
         * @return this builder
         * @throws IllegalArgumentException if {@code maxResults} is not greater than 0
         */
        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be greater than 0");
            }
            this.resultLimit = maxResults;
            return this;
        }

        /**
         * Builds a new {@link TavilyWebSearchDocumentRetriever} with the configured settings.
         *
         * @return a new retriever instance
         * @throws IllegalArgumentException if the client builder is null, the required API key
         *         environment variable is unset, or the result limit is not greater than 0
         */
        public TavilyWebSearchDocumentRetriever build() {
            return new TavilyWebSearchDocumentRetriever(clientBuilder, resultLimit);
        }
    }
}
