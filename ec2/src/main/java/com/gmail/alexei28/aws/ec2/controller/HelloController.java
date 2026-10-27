package com.gmail.alexei28.aws.ec2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

// http://localhost:8083/api/v1/hello
@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public String sayHello() {
        logger.info("Hello, Task, AWS EC2! Date: {}", LocalDateTime.now());
        return "Hello, Task, AWS EC2! Date: " + LocalDateTime.now();
    }
}