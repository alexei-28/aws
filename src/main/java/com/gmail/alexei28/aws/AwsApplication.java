package com.gmail.alexei28.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AwsApplication {
	private static final Logger logger = LoggerFactory.getLogger(AwsApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(AwsApplication.class, args);
		logger.info("\n\n ===== Application started successfully! =====\nAWS");
		logger.info(
				"\nJava version: {}, Java vendor: {}",
				System.getProperty("java.version"),
				System.getProperty("java.vendor"));
	}

}
