package com.gmail.alexei28.aws.task02.processor;

public interface DocumentStorage {
    byte[] download(String bucket, String objectKey);
}