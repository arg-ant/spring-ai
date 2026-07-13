package com.multimodel.llm.exception;

/**
 * Thrown when a model-generated answer fails fact-checking evaluation against a given
 * question (and, where applicable, its supporting context).
 */
public class InvalidAnswerException extends RuntimeException {

    /**
     * Creates a new exception describing which answer failed validation for which question.
     *
     * @param question the original question the answer was meant to address
     * @param answer the answer that failed the fact-checking evaluation
     */
    public InvalidAnswerException(String question, String answer) {
        super("Answer check failed: The answer \"" + answer + "\" " +
                "is not correct for the question \"" + question + "\".");
    }
}
