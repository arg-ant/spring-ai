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

public class PIIMaskingDocumentPostProcessor implements DocumentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PIIMaskingDocumentPostProcessor.class);

    // Regex patterns for common PII

    // Matches standard email formats: local-part@domain.tld
    // CASE_INSENSITIVE so mixed-case domains/local-parts still match
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
            Pattern.CASE_INSENSITIVE);

    // Matches common US-style phone formats, with optional country code,
    // optional parentheses around area code, and optional separators (-, ., space)
    // e.g. +1 (123) 456-7890, 123.456.7890, 1234567890
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b",
            Pattern.CASE_INSENSITIVE);

    // Replacement tokens substituted in place of detected PII
    private static final String EMAIL_REPLACEMENT = "[REDACTED_EMAIL]";
    private static final String PHONE_REPLACEMENT = "[REDACTED_PHONE]";

    // Private constructor forces instantiation through the static builder() factory method below,
    // rather than `new PIIMaskingDocumentPostProcessor()` directly
    private PIIMaskingDocumentPostProcessor() {
    }

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

    private String maskSensitiveInformation(String text) {
        String masked = text;
        // Mask emails — replace every regex match with the redaction token
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(EMAIL_REPLACEMENT);
        // Mask phone numbers — applied after email masking, independent passes over the string
        masked = PHONE_PATTERN.matcher(masked).replaceAll(PHONE_REPLACEMENT);
        return masked;
    }

    // Static factory method acting as the sole entry point for instantiation,
    // since the constructor is private.
    public static PIIMaskingDocumentPostProcessor builder() {
        return new PIIMaskingDocumentPostProcessor();
    }
}
