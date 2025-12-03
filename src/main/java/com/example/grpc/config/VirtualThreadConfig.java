package com.example.grpc.config;

import io.grpc.ServerBuilder;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Java 21 Virtual Threads Configuration for gRPC Server.
 * 
 * Virtual threads are lightweight threads that allow massive concurrency.
 * Instead of blocking a platform thread during I/O, virtual threads
 * unmount and allow the carrier thread to execute other virtual threads.
 * 
 * Benefits:
 * - Handle millions of concurrent gRPC requests
 * - Simple blocking code (no reactive programming needed)
 * - Automatic unmounting during blocking I/O
 * - Minimal memory footprint (~few KB vs ~1MB for platform threads)
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadConfig.class);

    /**
     * Virtual thread executor for Spring @Async operations.
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        log.info("Configuring Virtual Thread executor for Spring Async");
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Configure gRPC Server to use Virtual Threads.
     * 
     * Each gRPC request will be handled by a virtual thread,
     * allowing massive concurrency without platform thread overhead.
     */
    @Bean
    public GrpcServerConfigurer grpcServerConfigurer() {
        return serverBuilder -> {
            log.info("Configuring gRPC Server with Virtual Thread executor");
            if (serverBuilder instanceof ServerBuilder<?>) {
                serverBuilder.executor(Executors.newVirtualThreadPerTaskExecutor());
            }
        };
    }
}

