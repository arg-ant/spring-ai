package com.multimodel.llm.advisors;

import com.multimodel.llm.rag.PIIMaskingDocumentPostProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisor {

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder
    ) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(TranslationQueryTransformer
                        .builder()
                        .chatClientBuilder(chatClientBuilder.clone())
                        .targetLanguage("english").build())
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .documentRetriever(VectorStoreDocumentRetriever
                        .builder()
                        .similarityThreshold(0.5)
                        .topK(3)
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }

}
