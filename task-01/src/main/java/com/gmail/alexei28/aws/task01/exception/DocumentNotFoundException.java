package com.gmail.alexei28.aws.task01.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException() {
        super("Document not found");
    }
}
