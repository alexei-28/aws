package com.gmail.alexei28.aws.s3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskS3 {
    private static final Logger logger = LoggerFactory.getLogger(TaskS3.class);

    public static void main(String[] args) {
        SpringApplication.run(TaskS3.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nTask: AWS S3");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}