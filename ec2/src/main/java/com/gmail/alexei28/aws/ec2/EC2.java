package com.gmail.alexei28.aws.ec2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EC2 {
    private static final Logger logger = LoggerFactory.getLogger(EC2.class);

    public static void main(String[] args) {
        SpringApplication.run(EC2.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nAWS EC2");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}