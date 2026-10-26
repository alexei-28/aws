package com.gmail.alexei28.aws.ec2.controller;

import com.gmail.alexei28.aws.ec2.TaskEC2;
import com.gmail.alexei28.aws.ec2.dto.ServerInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@RestController
public class InfoController {
    @Value("${app.environment:Local}")
    private String environment;
    private static final Logger logger = LoggerFactory.getLogger(InfoController.class);

    @GetMapping("/info")
    public ServerInfoResponse info() throws UnknownHostException {
        logger.info("info, environment: {}", environment);
        return new ServerInfoResponse(
                LocalDateTime.now(),
                InetAddress.getLocalHost().getHostName(),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                environment

        );
    }
}