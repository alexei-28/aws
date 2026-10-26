package com.gmail.alexei28.aws.ec2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskEC2 {
    private static final Logger logger = LoggerFactory.getLogger(TaskEC2.class);

    public static void main(String[] args) {
        SpringApplication.run(TaskEC2.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nTask: AWS EC2");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}