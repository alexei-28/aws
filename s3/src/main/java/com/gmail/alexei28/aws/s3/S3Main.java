package com.gmail.alexei28.aws.s3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class S3Main {
    private static final Logger logger = LoggerFactory.getLogger(S3Main.class);

    public static void main(String[] args) {
        SpringApplication.run(S3Main.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nAWS S3");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}