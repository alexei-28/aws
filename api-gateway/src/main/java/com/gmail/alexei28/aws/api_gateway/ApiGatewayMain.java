package com.gmail.alexei28.aws.api_gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayMain {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayMain.class);

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayMain.class, args);

        logger.info("\n\n ===== Application started successfully! =====\nAWS API Gateway");
        logger.info(
                "\nJava version: {}, Java vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
    }

}