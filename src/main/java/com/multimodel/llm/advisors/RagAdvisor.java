package com.multimodel.llm.advisors;

import com.multimodel.llm.config.ChatClientFactory;
import com.multimodel.llm.rag.PIIMaskingDocumentPostProcessor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.multimodel.llm.config.Constants.*;

/**
 * Configuration providing the {@link RetrievalAugmentationAdvisor} bean used to
 * add retrieval-augmented generation (RAG) to chat requests.
 */
@Configuration
public class RagAdvisor {

    /**
     * Builds the RAG advisor, wiring together query translation, PII masking of
     * retrieved documents, and vector-store-backed document retrieval.
     *
     * @param vectorStore       the vector store to retrieve candidate documents from
     * @param chatClientFactory factory used to create the chat client that translates
     *                          queries into {@link com.multimodel.llm.config.Constants#LANGUAGE}
     * @return a configured {@link RetrievalAugmentationAdvisor}
     */
    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ChatClientFactory chatClientFactory
    ) {
        return RetrievalAugmentationAdvisor.builder()
                // Translate the incoming query into LANGUAGE before retrieval,
                // so documents are matched consistently regardless of the user's input language
                .queryTransformers(TranslationQueryTransformer
                        .builder()
                        .chatClientBuilder(chatClientFactory.createOllama())
                        .targetLanguage(LANGUAGE).build())
                // Strip/mask personally identifiable information from retrieved documents
                // before they're injected into the prompt context
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                // Retrieve the top K most similar documents from the vector store,
                // discarding any below the similarity threshold
                .documentRetriever(VectorStoreDocumentRetriever
                        .builder()
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .topK(TOP_K)
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }

}
