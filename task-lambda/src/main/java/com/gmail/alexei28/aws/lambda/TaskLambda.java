package com.gmail.alexei28.aws.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskLambda {
    private static final Logger logger = LoggerFactory.getLogger(TaskLambda.class);

    public static void main(String[] args) {
        SpringApplication.run(TaskLambda.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nTask: AWS Lambda");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}