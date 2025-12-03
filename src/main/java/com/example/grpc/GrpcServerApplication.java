package com.example.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * gRPC Server Application with Java 21 Virtual Threads.
 * 
 * This is a standalone gRPC server that exposes:
 * - Port 9090: gRPC Server
 * - Port 8081: Health check endpoint (for ALB)
 */
@SpringBootApplication
public class GrpcServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcServerApplication.class, args);
    }
}

