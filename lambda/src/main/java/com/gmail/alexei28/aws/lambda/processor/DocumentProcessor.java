package com.gmail.alexei28.aws.lambda.processor;

import com.gmail.alexei28.aws.lambda.domain.DocumentJob;

public interface DocumentProcessor {
    void process(DocumentJob job);
}