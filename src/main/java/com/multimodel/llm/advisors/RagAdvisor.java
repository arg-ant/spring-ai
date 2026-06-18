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

@Configuration
public class RagAdvisor {

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ChatClientFactory chatClientFactory
    ) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(TranslationQueryTransformer
                        .builder()
                        .chatClientBuilder(chatClientFactory.createOllama())
                        .targetLanguage(LANGUAGE).build())
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .documentRetriever(VectorStoreDocumentRetriever
                        .builder()
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .topK(TOP_K)
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }

}
