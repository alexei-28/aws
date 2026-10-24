package com.gmail.alexei28.aws.task02.processor;

import com.gmail.alexei28.aws.task02.domain.DocumentJob;

public interface DocumentProcessor {
    void process(DocumentJob job);
}