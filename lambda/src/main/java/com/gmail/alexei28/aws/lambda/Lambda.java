package com.gmail.alexei28.aws.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Lambda {
    private static final Logger logger = LoggerFactory.getLogger(Lambda.class);

    public static void main(String[] args) {
        SpringApplication.run(Lambda.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nAWS Lambda");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}