package com.multimodel.llm.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A {@link DocumentPostProcessor} that redacts personally identifiable information (PII) —
 * email addresses and phone numbers — from retrieved documents before they are passed
 * further down the RAG pipeline (e.g. into a prompt).
 * <p>
 * Instances are obtained via the static {@link #builder()} factory method rather than a
 * public constructor.
 */
public class PIIMaskingDocumentPostProcessor implements DocumentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PIIMaskingDocumentPostProcessor.class);

    /**
     * Matches standard email formats: local-part@domain.tld.
     * Case-insensitive so mixed-case domains/local-parts still match.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches common US-style phone formats, with optional country code, optional
     * parentheses around the area code, and optional separators ({@code -}, {@code .}, space).
     * Examples: {@code +1 (123) 456-7890}, {@code 123.456.7890}, {@code 1234567890}.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b",
            Pattern.CASE_INSENSITIVE);

    // Replacement tokens substituted in place of detected PII
    private static final String EMAIL_REPLACEMENT = "[REDACTED_EMAIL]";
    private static final String PHONE_REPLACEMENT = "[REDACTED_PHONE]";

    /**
     * Private constructor; use {@link #builder()} to obtain an instance.
     */
    private PIIMaskingDocumentPostProcessor() {
    }

    /**
     * Masks emails and phone numbers found in the text of each document.
     *
     * @param query the query the documents were retrieved for (used only for logging)
     * @param documents the retrieved documents to mask; must be non-null and contain no null elements
     * @return a new list of documents with masked text and a {@code pii_masked} metadata flag set to
     *         {@code true}; the input list is returned unchanged if it is empty
     * @throws IllegalArgumentException if {@code query} or {@code documents} is null, or if
     *         {@code documents} contains a null element
     */
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        // Defensive guards: fail fast on invalid input rather than NPE deeper in the stream
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");
        Assert.noNullElements(documents, "documents cannot contain null elements");

        // Nothing to process — short-circuit and return as-is
        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        logger.debug("Masking sensitive information in documents for query: {}", query.text());

        return documents.stream()
                .map(document -> {
                    // Guard against null text (Document.getText() may return null for some sources)
                    String text = document.getText() != null ? document.getText() : "";

                    // Apply PII masking
                    String maskedText = maskSensitiveInformation(text);

                    // Document is immutable: use mutate()/build() to produce a new instance
                    // with masked text and a metadata flag marking it as processed
                    return document.mutate()
                            .text(maskedText)
                            .metadata("pii_masked", true)
                            .build();
                })
                .toList();
    }

    /**
     * Replaces detected email addresses and phone numbers in the given text with redaction
     * tokens.
     *
     * @param text the text to mask
     * @return the text with all detected emails and phone numbers replaced by their
     *         respective redaction tokens
     */
    private String maskSensitiveInformation(String text) {
        String masked = text;
        // Mask emails — replace every regex match with the redaction token
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT);
        // Mask phone numbers — applied after email masking, independent passes over the string
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT);
        return masked;
    }

    /**
     * Creates a new {@link PIIMaskingDocumentPostProcessor}.
     *
     * @return a new instance ready to mask PII in retrieved documents
     */
    public static PIIMaskingDocumentPostProcessor builder() {
        return new PIIMaskingDocumentPostProcessor();
    }
}
