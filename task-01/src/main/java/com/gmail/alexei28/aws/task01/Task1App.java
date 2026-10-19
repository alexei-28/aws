package com.gmail.alexei28.aws.task01;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class Task1App {
    private static final Logger logger = LoggerFactory.getLogger(Task1App.class);

    public static void main(String[] args) {
        SpringApplication.run(Task1App.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nTask 01: AWS S3");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}