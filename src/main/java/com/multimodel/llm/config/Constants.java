package com.multimodel.llm.config;

/**
 * Shared default values used across chat client, RAG, image generation, and
 * ticketing configuration.
 */
public class Constants {

    /** Default sampling temperature applied to chat model requests. */
    public static double TEMPERATURE = 0.7;

    /** Default maximum number of tokens a chat model may generate in a response. */
    public static int MAX_TOKENS = 250;

    /** Model name of the bespoke Ollama fact-checking model used for hallucination detection. */
    public static String BESPOKE_MINICHECK_MODEL = "bespoke-minicheck";

    /** Maximum number of messages retained in the sliding-window chat memory. */
    public static int MAX_MESSAGES = 10;

    /** Target language queries are translated into before retrieval-augmented generation. */
    public static String LANGUAGE = "english";

    /** Maximum number of documents returned by vector store similarity search. */
    public static int TOP_K = 3;

    /** Minimum similarity score a document must meet to be returned by vector store search. */
    public static double SIMILARITY_THRESHOLD = 0.5;

    /** Number of images requested per image generation call. */
    public static int IMAGES_NR = 1;

    /** Requested image quality passed to the image generation API. */
    public static String IMAGE_QUALITY = "auto";

    /** Height, in pixels, of generated images. */
    public static int IMAGE_HEIGHT = 1024;

    /** Width, in pixels, of generated images. */
    public static int IMAGE_WIDTH = 1024;

    /** Placeholder name for the customer's name in prompt templates. */
    public static String CUSTOMER_NAME_PLACEHOLDER = "customerName";

    /** Placeholder name for the customer's message in prompt templates. */
    public static String CUSTOMER_MESSAGE_PLACEHOLDER = "customerMessage";

    /** Placeholder name for retrieved RAG context in prompt templates. */
    public static String DOCUMENTS_PLACEHOLDER = "documents";

    /** Target chunk size, in tokens, used when splitting documents for embedding. */
    public static int CHUNK_SIZE = 200;

    /** Maximum number of chunks a single document may be split into. */
    public static int MAX_NUM_CHUNKS = 400;

    /** Status assigned to newly created help desk tickets. */
    public static String TICKET_STATUS_OPEN = "OPEN";

    /** Status assigned to in progress help desk tickets. */
    public static String TICKET_STATUS_IN_PROGRESS = "IN_PROGRESS";

    /** Maximum number of results returned by web search retrieval. */
    public static int MAX_NUM_WEB_SEARCH_RESULTS = 3;
}
