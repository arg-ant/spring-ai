package com.multimodel.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.multimodel.llm.config.Constants.CHUNK_SIZE;
import static com.multimodel.llm.config.Constants.MAX_NUM_CHUNKS;

/**
 * Loads and removes sample data in the {@link VectorStore} on demand: a fixed set of
 * unrelated, general-knowledge sentences for use as sample/test data in RAG retrieval, and
 * the HR policies PDF document.
 * <p>
 * Each batch of documents is tagged with a {@code source} metadata value when added, so it
 * can be selectively deleted later without affecting other data in the vector store.
 */
@Service
public class VectorStoreDataService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreDataService.class);

    /**
     * Metadata {@code source} value tagging documents added by {@link #loadRandom()}.
     */
    private static final String RANDOM_DATA = "random-data";

    /**
     * Metadata {@code source} value tagging documents added by {@link #loadPDF()}.
     */
    private static final String PDF_DATA = "hr-policy-pdf";

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
     * Creates a new service backed by the given vector store.
     *
     * @param vectorStore the vector store where document embeddings will be saved and removed
     */
    public VectorStoreDataService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Wraps each sample sentence in a {@link Document} tagged with the {@link #RANDOM_DATA}
     * source, and stores it, along with its embedding, in the vector store.
     */
    public void loadRandom() {
        List<Document> documents = getSentences().stream()
                .map(text -> {
                    Document doc = new Document(text);
                    doc.getMetadata().put("source", RANDOM_DATA);
                    return doc;
                })
                .toList();

        vectorStore.add(documents);
    }

    /**
     * Deletes all documents previously added by {@link #loadRandom()} from the vector store,
     * identified by their {@link #RANDOM_DATA} source metadata.
     */
    public void removeRandom() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("source", RANDOM_DATA)
                .build();
        vectorStore.delete(filterExpression);
    }

    /**
     * Reads the HR policies PDF, splits it into token-sized chunks, tags each chunk with the
     * {@link #PDF_DATA} source, and stores the chunks, along with their embeddings, in the
     * vector store.
     */
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
        chunks.forEach(chunk -> chunk.getMetadata().put("source", PDF_DATA));
        vectorStore.add(chunks);

        logger.info("Loaded {} HR policy chunks into the vector store", chunks.size());
    }

    /**
     * Deletes all documents previously added by {@link #loadPDF()} from the vector store,
     * identified by their {@link #PDF_DATA} source metadata.
     */
    public void removePDF() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("source", PDF_DATA)
                .build();
        vectorStore.delete(filterExpression);
    }

    /**
     * Returns the fixed set of unrelated, general-knowledge sentences used as sample/test
     * data by {@link #loadRandom()}.
     *
     * @return the sample sentences
     */
    private List<String> getSentences() {
        return List.of(
                "Java is used for building scalable enterprise applications.",
                "Python is commonly used for machine learning and automation tasks.",
                "JavaScript is essential for creating interactive web pages.",
                "Docker packages applications into lightweight containers.",
                "Kubernetes automates container orchestration at scale.",
                "Redis is an in-memory data store used for caching.",
                "PostgreSQL supports complex queries and full ACID compliance.",
                "Kafka is a distributed event streaming platform.",
                "REST APIs allow stateless client-server communication.",
                "GraphQL enables clients to fetch exactly the data they need.",
                "Credit scores influence the interest rates on loans.",
                "Mutual funds pool money from investors to buy securities.",
                "Bitcoin operates on a decentralized peer-to-peer network.",
                "Ethereum supports smart contract deployment.",
                "The stock market opens at 9:30 a.m. EST on weekdays.",
                "Compound interest increases investment returns over time.",
                "Diversifying investments reduces overall risk.",
                "A blockchain is a distributed, immutable ledger of transactions.",
                "Photosynthesis is how plants convert sunlight into energy.",
                "The water cycle involves evaporation, condensation, and precipitation.",
                "The ozone layer protects Earth from harmful ultraviolet rays.",
                "Earth revolves around the Sun in an elliptical orbit.",
                "Lightning is a discharge of electricity caused by charged clouds.",
                "DNA is the molecule that carries genetic instructions in living organisms.",
                "Volcanoes form when magma rises through Earth's crust.",
                "Earthquakes are caused by sudden tectonic shifts.",
                "The Sahara is the largest hot desert in the world.",
                "Mount Kilimanjaro is the tallest mountain in Africa.",
                "Japan is known for its cherry blossoms and advanced technology.",
                "The Great Wall of China is over 13,000 miles long.",
                "Niagara Falls is located between Canada and the U.S.",
                "The Amazon River is the second longest river in the world.",
                "Oats are high in fiber and help reduce cholesterol.",
                "Drinking water improves digestion and skin health.",
                "A balanced diet includes proteins, carbs, fats, and vitamins.",
                "Broccoli is rich in vitamins A, C, and K.",
                "Green tea contains antioxidants beneficial for metabolism.",
                "Too much sugar increases the risk of diabetes.",
                "Walking 30 minutes a day improves cardiovascular health.",
                "Meditation can reduce stress and improve focus.",
                "Gratitude journaling is linked to higher happiness levels.",
                "Deep breathing exercises help regulate anxiety.",
                "Reading daily improves vocabulary and cognitive function.",
                "Setting daily goals increases productivity.",
                "STEM stands for Science, Technology, Engineering, and Mathematics.",
                "Bloom’s taxonomy categorizes educational goals.",
                "Project-based learning enhances student engagement.",
                "Online courses offer flexibility for remote learners.",
                "Flashcards are effective for memorizing vocabulary.",
                "Agile methodology promotes iterative software development.",
                "OKRs help align team goals with business strategy.",
                "Remote work offers flexibility but requires clear communication.",
                "CRM systems manage customer relationships and sales pipelines.",
                "SWOT analysis identifies strengths, weaknesses, opportunities, and threats."
        );
    }
}
