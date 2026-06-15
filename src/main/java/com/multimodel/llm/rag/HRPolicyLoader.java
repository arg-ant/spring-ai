package com.multimodel.llm.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HRPolicyLoader {

    private final VectorStore vectorStore;

    public HRPolicyLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Value("classpath:Eazybytes_HR_Policies.pdf")
    Resource policyFile;

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

                        .withChunkSize(200)
                        // Split the document into chunks of approximately 200 tokens
                        // Smaller chunks typically improve retrieval quality

                        .withMaxNumChunks(400)
                        // Limit the total number of generated chunks
                        // Prevents very large documents from creating excessive embeddings

                        .build();

        // Split documents into chunks
        // Generate embeddings for each chunk using mxbai-embed-large
        // Store chunks and embeddings in Qdrant
        vectorStore.add(textSplitter.split(documents));
    }
}
