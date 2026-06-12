package com.multimodel.llm.exception;

public class InvalidAnswerException extends RuntimeException {

    public InvalidAnswerException(String question, String answer) {
        super("Answer check failed: The answer \"" + answer + "\" " +
                "is not correct for the question \"" + question + "\".");
    }
}
