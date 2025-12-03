package com.example.grpc.service;

import com.example.grpc.proto.GreetingServiceGrpc;
import com.example.grpc.proto.HelloRequest;
import com.example.grpc.proto.HelloResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * gRPC Service Implementation with all 4 communication patterns.
 * Runs on Java 21 Virtual Threads for maximum concurrency.
 */
@GrpcService
public class GreetingGrpcService extends GreetingServiceGrpc.GreetingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GreetingGrpcService.class);

    private static final Map<String, String> GREETINGS = Map.of(
            "en", "Hello",
            "es", "Hola",
            "fr", "Bonjour",
            "de", "Hallo",
            "it", "Ciao",
            "ja", "こんにちは",
            "ko", "안녕하세요",
            "zh", "你好"
    );

    // ==================== 1. UNARY RPC ====================
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        log.info("[UNARY] Request for: {} on {}", request.getName(), Thread.currentThread());

        simulateWork(50);

        HelloResponse response = buildResponse(request.getName(), request.getLanguage());
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        
        log.info("[UNARY] Completed for: {}", request.getName());
    }

    // ==================== 2. SERVER STREAMING RPC ====================
    @Override
    public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        log.info("[SERVER_STREAM] Request for: {} on {}", request.getName(), Thread.currentThread());

        String name = request.getName();
        
        GREETINGS.forEach((lang, greeting) -> {
            simulateWork(30);
            
            HelloResponse response = HelloResponse.newBuilder()
                    .setMessage(greeting + ", " + name + "! [" + lang + "]")
                    .setTimestamp(Instant.now().toString())
                    .setThreadInfo(getThreadInfo())
                    .build();
            
            responseObserver.onNext(response);
        });

        responseObserver.onCompleted();
        log.info("[SERVER_STREAM] Completed - sent {} greetings", GREETINGS.size());
    }

    // ==================== 3. CLIENT STREAMING RPC ====================
    @Override
    public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloResponse> responseObserver) {
        log.info("[CLIENT_STREAM] Started on {}", Thread.currentThread());

        return new StreamObserver<>() {
            private final List<String> names = new ArrayList<>();

            @Override
            public void onNext(HelloRequest request) {
                log.debug("[CLIENT_STREAM] Received: {}", request.getName());
                names.add(request.getName());
                simulateWork(20);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[CLIENT_STREAM] Error", t);
            }

            @Override
            public void onCompleted() {
                String allNames = String.join(", ", names);
                
                HelloResponse response = HelloResponse.newBuilder()
                        .setMessage("Hello to all " + names.size() + " friends: " + allNames + "!")
                        .setTimestamp(Instant.now().toString())
                        .setThreadInfo(getThreadInfo())
                        .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                
                log.info("[CLIENT_STREAM] Completed - received {} names", names.size());
            }
        };
    }

    // ==================== 4. BIDIRECTIONAL STREAMING RPC ====================
    @Override
    public StreamObserver<HelloRequest> sayHelloBidirectional(StreamObserver<HelloResponse> responseObserver) {
        log.info("[BIDI_STREAM] Started on {}", Thread.currentThread());

        return new StreamObserver<>() {
            private int count = 0;

            @Override
            public void onNext(HelloRequest request) {
                count++;
                log.debug("[BIDI_STREAM] Received #{}: {}", count, request.getName());
                
                simulateWork(25);
                
                HelloResponse response = buildResponse(request.getName(), request.getLanguage());
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[BIDI_STREAM] Error", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                log.info("[BIDI_STREAM] Completed - processed {} messages", count);
            }
        };
    }

    // ==================== HELPERS ====================
    
    private HelloResponse buildResponse(String name, String language) {
        String lang = (language == null || language.isEmpty()) ? "en" : language;
        String greeting = GREETINGS.getOrDefault(lang, GREETINGS.get("en"));

        return HelloResponse.newBuilder()
                .setMessage(greeting + ", " + name + "!")
                .setTimestamp(Instant.now().toString())
                .setThreadInfo(getThreadInfo())
                .build();
    }

    private String getThreadInfo() {
        Thread current = Thread.currentThread();
        return String.format("Thread[name=%s, virtual=%s, id=%d]",
                current.getName(), current.isVirtual(), current.threadId());
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
