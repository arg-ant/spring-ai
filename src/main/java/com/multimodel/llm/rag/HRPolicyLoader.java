package com.multimodel.llm.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.List;

import static com.multimodel.llm.config.Constants.CHUNK_SIZE;
import static com.multimodel.llm.config.Constants.MAX_NUM_CHUNKS;

/**
 * Loads the HR policies PDF into the {@link VectorStore} on application startup so it can be
 * retrieved for RAG (retrieval-augmented generation) queries.
 * <p>
 * Currently disabled as a Spring bean (the {@code @Component} annotation is commented out);
 * enable it to have the policy document indexed automatically at startup.
 */
//@Component
public class HRPolicyLoader {

    private static final Logger logger = LoggerFactory.getLogger(HRPolicyLoader.class);

    /**
     * The HR policies PDF resource, resolved from the classpath.
     */
    @Value("classpath:HR_Policies.pdf")
    private Resource policyFile;

    /**
     * Vector store used to persist embedded document chunks for later retrieval.
     */
    private final VectorStore vectorStore;

    /**
     * Creates a new loader backed by the given vector store.
     *
     * @param vectorStore the vector store where document embeddings will be saved
     */
    public HRPolicyLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Reads the HR policies PDF, splits it into token-based chunks, and stores the resulting
     * embeddings in the vector store.
     * <p>
     * Executed automatically once after Spring creates the bean, since this method is
     * annotated with {@link PostConstruct}.
     */
    @PostConstruct
    public void loadPDF() {

        // Create a document reader that uses Apache Tika to extract text from the PDF file
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(policyFile);

        // Read the PDF and convert its contents into Spring AI Document objects
        List<Document> documents = tikaDocumentReader.read();

        TextSplitter textSplitter =
                TokenTextSplitter.builder()

                        .withChunkSize(CHUNK_SIZE)
                        // Split the document into chunks of approximately 200 tokens
                        // Smaller chunks typically improve retrieval quality

                        .withMaxNumChunks(MAX_NUM_CHUNKS)
                        // Limit the total number of generated chunks
                        // Prevents very large documents from creating excessive embeddings

                        .build();

        // Split documents into chunks
        // Generate embeddings for each chunk using embedding agent
        // Store chunks and embeddings in Qdrant
        List<Document> chunks = textSplitter.split(documents);
        vectorStore.add(chunks);

        logger.info("Loaded {} HR policy chunks into the vector store", chunks.size());
    }
}
