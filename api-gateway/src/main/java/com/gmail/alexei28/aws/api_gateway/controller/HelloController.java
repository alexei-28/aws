package com.gmail.alexei28.aws.api_gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// http://localhost:8084/api/v1/hello
@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public ResponseEntity<?> sayHello() {
        String message = "Hello, AWS API Gateway! Current date: " + LocalDateTime.now();
        logger.info("sayHello, {}", message);

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}