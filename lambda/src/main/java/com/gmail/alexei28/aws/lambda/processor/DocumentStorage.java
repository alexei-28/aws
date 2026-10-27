package com.gmail.alexei28.aws.lambda.processor;

public interface DocumentStorage {
    byte[] download(String bucket, String objectKey);
}