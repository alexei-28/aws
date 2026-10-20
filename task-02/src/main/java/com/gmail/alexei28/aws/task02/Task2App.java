package com.gmail.alexei28.aws.task02;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Task2App {
    private static final Logger logger = LoggerFactory.getLogger(Task2App.class);

    public static void main(String[] args) {
        SpringApplication.run(Task2App.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nTask 02: AWS Lambda");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}