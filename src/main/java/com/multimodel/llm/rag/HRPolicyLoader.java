package com.multimodel.llm.rag;

import jakarta.annotation.PostConstruct;
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

//@Component
public class HRPolicyLoader {

    @Value("classpath:HR_Policies.pdf")
    private Resource policyFile;

    private final VectorStore vectorStore;

    public HRPolicyLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // Execute this method automatically once after Spring creates the bean
    // Commonly used for initialization logic such as loading data into a vector store
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
        // Generate embeddings for each chunk using mxbai-embed-large
        // Store chunks and embeddings in Qdrant
        vectorStore.add(textSplitter.split(documents));
    }
}
