package com.gmail.alexei28.aws.dbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbsMain {
    private static final Logger logger = LoggerFactory.getLogger(DbsMain.class);

    public static void main(String[] args) {
        SpringApplication.run(DbsMain.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nAWS DBs");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }
}
