# How to Call gRPC Service from Another Microservice

## Overview

Your gRPC Server exposes the `GreetingService` on **port 9090**. Any microservice can call it using a gRPC client.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS VPC                                         │
│                                                                              │
│   ┌─────────────────────────┐         ┌─────────────────────────────────┐   │
│   │   Your Microservice     │         │     gRPC Server (This Project)  │   │
│   │   (gRPC Client)         │         │                                 │   │
│   │                         │  gRPC   │   Port 9090                     │   │
│   │   - Order Service       │────────►│   - GreetingService             │   │
│   │   - User Service        │  HTTP/2 │   - SayHello                    │   │
│   │   - Payment Service     │         │   - SayHelloServerStream        │   │
│   │   - Any Service...      │         │   - SayHelloClientStream        │   │
│   │                         │         │   - SayHelloBidirectional       │   │
│   └─────────────────────────┘         └─────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Step 1: Copy the Proto File

Copy `greeting.proto` to your client microservice:

```
your-microservice/
└── src/main/proto/
    └── greeting.proto    ← Copy this file
```

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.yourcompany.yourservice.proto";  // Change package
option java_outer_classname = "GreetingProto";

package greeting;

service GreetingService {
    rpc SayHello (HelloRequest) returns (HelloResponse);
    rpc SayHelloServerStream (HelloRequest) returns (stream HelloResponse);
    rpc SayHelloClientStream (stream HelloRequest) returns (HelloResponse);
    rpc SayHelloBidirectional (stream HelloRequest) returns (stream HelloResponse);
}

message HelloRequest {
    string name = 1;
    string language = 2;
}

message HelloResponse {
    string message = 1;
    string timestamp = 2;
    string thread_info = 3;
}
```

---

## Step 2: Add Dependencies (pom.xml)

```xml
<properties>
    <grpc.version>1.60.0</grpc.version>
    <protobuf.version>3.25.1</protobuf.version>
    <grpc-spring-boot.version>3.0.0.RELEASE</grpc-spring-boot.version>
</properties>

<dependencies>
    <!-- gRPC Client Starter -->
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-client-spring-boot-starter</artifactId>
        <version>${grpc-spring-boot.version}</version>
    </dependency>

    <!-- gRPC & Protobuf -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>${grpc.version}</version>
    </dependency>
    
    <dependency>
        <groupId>javax.annotation</groupId>
        <artifactId>javax.annotation-api</artifactId>
        <version>1.3.2</version>
    </dependency>
</dependencies>

<!-- Protobuf Plugin -->
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Step 3: Configure Client (application.yml)

```yaml
grpc:
  client:
    greeting-service:
      # Local development
      address: static://localhost:9090
      negotiation-type: plaintext
      
      # For AWS (Internal ALB)
      # address: dns:///grpc-server.internal.yourcompany.com:443
      # negotiation-type: tls
      
      enable-keep-alive: true
      keep-alive-time: 30s
      keep-alive-timeout: 10s
```

---

## Step 4: Create gRPC Client Service

```java
package com.yourcompany.yourservice.client;

import com.yourcompany.yourservice.proto.GreetingServiceGrpc;
import com.yourcompany.yourservice.proto.HelloRequest;
import com.yourcompany.yourservice.proto.HelloResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class GreetingGrpcClient {

    // Inject gRPC stubs
    @GrpcClient("greeting-service")
    private GreetingServiceGrpc.GreetingServiceBlockingStub blockingStub;

    @GrpcClient("greeting-service")
    private GreetingServiceGrpc.GreetingServiceStub asyncStub;

    // ==================== 1. UNARY ====================
    public HelloResponse sayHello(String name, String language) {
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .setLanguage(language)
                .build();

        return blockingStub.sayHello(request);
    }

    // ==================== 2. SERVER STREAMING ====================
    public List<HelloResponse> sayHelloServerStream(String name) {
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();

        List<HelloResponse> responses = new ArrayList<>();
        Iterator<HelloResponse> iterator = blockingStub.sayHelloServerStream(request);
        
        while (iterator.hasNext()) {
            responses.add(iterator.next());
        }
        
        return responses;
    }

    // ==================== 3. CLIENT STREAMING ====================
    public HelloResponse sayHelloClientStream(List<String> names) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final HelloResponse[] result = new HelloResponse[1];

        StreamObserver<HelloResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse response) {
                result[0] = response;
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<HelloRequest> requestObserver = 
            asyncStub.sayHelloClientStream(responseObserver);

        for (String name : names) {
            requestObserver.onNext(
                HelloRequest.newBuilder().setName(name).build()
            );
        }
        
        requestObserver.onCompleted();
        latch.await(30, TimeUnit.SECONDS);
        
        return result[0];
    }

    // ==================== 4. BIDIRECTIONAL ====================
    public List<HelloResponse> sayHelloBidirectional(List<String> names) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<HelloResponse> responses = new ArrayList<>();

        StreamObserver<HelloResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse response) {
                responses.add(response);
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<HelloRequest> requestObserver = 
            asyncStub.sayHelloBidirectional(responseObserver);

        for (String name : names) {
            requestObserver.onNext(
                HelloRequest.newBuilder().setName(name).build()
            );
        }
        
        requestObserver.onCompleted();
        latch.await(30, TimeUnit.SECONDS);
        
        return responses;
    }
}
```

---

## Step 5: Use in Your Service

```java
package com.yourcompany.yourservice.service;

import com.yourcompany.yourservice.client.GreetingGrpcClient;
import com.yourcompany.yourservice.proto.HelloResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final GreetingGrpcClient greetingClient;

    public OrderService(GreetingGrpcClient greetingClient) {
        this.greetingClient = greetingClient;
    }

    public void processOrder(String customerName) {
        // Call gRPC service
        HelloResponse greeting = greetingClient.sayHello(customerName, "en");
        
        System.out.println("Greeting: " + greeting.getMessage());
        System.out.println("Timestamp: " + greeting.getTimestamp());
        System.out.println("Thread: " + greeting.getThreadInfo());
    }

    public void notifyMultipleCustomers(List<String> customers) throws InterruptedException {
        // Use bidirectional streaming
        List<HelloResponse> responses = greetingClient.sayHelloBidirectional(customers);
        
        responses.forEach(r -> 
            System.out.println("Notified: " + r.getMessage())
        );
    }
}
```

---

## Communication Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         gRPC Call Flow                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Your Microservice                              gRPC Server                  │
│  ─────────────────                              ───────────                  │
│                                                                              │
│  ┌─────────────────────┐                    ┌─────────────────────┐         │
│  │  OrderService       │                    │  GreetingGrpcService│         │
│  │                     │                    │                     │         │
│  │  processOrder()     │                    │  sayHello()         │         │
│  └──────────┬──────────┘                    └──────────▲──────────┘         │
│             │                                          │                     │
│             ▼                                          │                     │
│  ┌─────────────────────┐    HelloRequest    ┌─────────┴───────────┐         │
│  │  GreetingGrpcClient │ ──────────────────►│  gRPC Server        │         │
│  │                     │                    │  (Port 9090)        │         │
│  │  @GrpcClient        │◄────────────────── │                     │         │
│  │  blockingStub       │    HelloResponse   │  Virtual Threads    │         │
│  └─────────────────────┘                    └─────────────────────┘         │
│                                                                              │
│  Protocol: HTTP/2                                                           │
│  Serialization: Protocol Buffers (binary, efficient)                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## AWS Configuration

### Local Development

```yaml
grpc:
  client:
    greeting-service:
      address: static://localhost:9090
      negotiation-type: plaintext
```

### AWS with Internal ALB

```yaml
grpc:
  client:
    greeting-service:
      address: dns:///grpc-internal-alb.yourcompany.internal:443
      negotiation-type: tls
```

### AWS Service Discovery (ECS/EKS)

```yaml
grpc:
  client:
    greeting-service:
      address: dns:///grpc-server.production.local:9090
      negotiation-type: plaintext  # Within VPC
```

---

## Network Diagram (AWS)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS VPC                                         │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         Private Subnet                                 │  │
│  │                                                                        │  │
│  │  ┌─────────────────┐         ┌─────────────────┐                      │  │
│  │  │ Order Service   │         │ User Service    │                      │  │
│  │  │ (ECS Task)      │         │ (ECS Task)      │                      │  │
│  │  │                 │         │                 │                      │  │
│  │  │ gRPC Client     │         │ gRPC Client     │                      │  │
│  │  └────────┬────────┘         └────────┬────────┘                      │  │
│  │           │                           │                                │  │
│  │           │         gRPC              │                                │  │
│  │           └───────────┬───────────────┘                                │  │
│  │                       │                                                │  │
│  │                       ▼                                                │  │
│  │           ┌───────────────────────┐                                   │  │
│  │           │    Internal ALB       │                                   │  │
│  │           │    (gRPC - HTTP/2)    │                                   │  │
│  │           └───────────┬───────────┘                                   │  │
│  │                       │                                                │  │
│  │                       ▼                                                │  │
│  │           ┌───────────────────────┐                                   │  │
│  │           │    gRPC Server        │                                   │  │
│  │           │    (This Project)     │                                   │  │
│  │           │    Port 9090          │                                   │  │
│  │           └───────────────────────┘                                   │  │
│  │                                                                        │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

| Pattern | Client Method | Use Case |
|---------|--------------|----------|
| **Unary** | `blockingStub.sayHello()` | Simple request/response |
| **Server Stream** | `blockingStub.sayHelloServerStream()` | Download, notifications |
| **Client Stream** | `asyncStub.sayHelloClientStream()` | Upload, batch processing |
| **Bidirectional** | `asyncStub.sayHelloBidirectional()` | Chat, real-time sync |

---

## Error Handling

```java
import io.grpc.StatusRuntimeException;
import io.grpc.Status;

try {
    HelloResponse response = blockingStub.sayHello(request);
} catch (StatusRuntimeException e) {
    Status status = e.getStatus();
    
    switch (status.getCode()) {
        case UNAVAILABLE:
            // Server is down, retry or failover
            break;
        case DEADLINE_EXCEEDED:
            // Timeout
            break;
        case INVALID_ARGUMENT:
            // Bad request
            break;
        default:
            // Handle other errors
    }
}
```

---

## Summary

1. **Copy proto file** to your microservice
2. **Add dependencies** (grpc-client-spring-boot-starter)
3. **Configure** client address in application.yml
4. **Create client** service with `@GrpcClient`
5. **Inject and use** in your business logic

