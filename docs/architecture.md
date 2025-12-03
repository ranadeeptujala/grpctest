# gRPC Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [gRPC Communication Patterns](#grpc-communication-patterns)
4. [Virtual Threads Architecture](#virtual-threads-architecture)
5. [Component Details](#component-details)
6. [Data Flow](#data-flow)
7. [AWS Deployment Architecture](#aws-deployment-architecture)

---

## Overview

This application demonstrates a modern microservices architecture using:

- **Java 21 Virtual Threads** - Lightweight concurrency model
- **gRPC** - High-performance RPC framework
- **Spring Boot 3.x** - Enterprise application framework
- **Protocol Buffers** - Efficient serialization

### Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    Technology Stack                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Java 21   │  │ Spring Boot │  │        gRPC         │  │
│  │   Virtual   │  │    3.2.x    │  │    1.60.x           │  │
│  │   Threads   │  │             │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Protocol   │  │   Netty     │  │   grpc-spring-boot  │  │
│  │  Buffers    │  │  (HTTP/2)   │  │      starter        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring Boot Application                            │
│                                                                              │
│  ┌────────────────────────────────┐    ┌────────────────────────────────┐   │
│  │         REST Layer             │    │         gRPC Layer             │   │
│  │         (Port 8080)            │    │         (Port 9090)            │   │
│  │                                │    │                                │   │
│  │  ┌──────────────────────────┐  │    │  ┌──────────────────────────┐  │   │
│  │  │   GreetingController     │  │    │  │   GreetingGrpcService    │  │   │
│  │  │   HealthController       │  │    │  │   (Proto Implementation) │  │   │
│  │  └───────────┬──────────────┘  │    │  └───────────▲──────────────┘  │   │
│  │              │                 │    │              │                 │   │
│  │              ▼                 │    │              │                 │   │
│  │  ┌──────────────────────────┐  │    │  ┌──────────────────────────┐  │   │
│  │  │   GreetingGrpcClient     │──┼────┼─►│   gRPC Server            │  │   │
│  │  │   (Blocking & Async)     │  │    │  │   (Netty + HTTP/2)       │  │   │
│  │  └──────────────────────────┘  │    │  └──────────────────────────┘  │   │
│  │                                │    │                                │   │
│  └────────────────────────────────┘    └────────────────────────────────┘   │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     Virtual Thread Executor                           │   │
│  │           (Handles all HTTP & gRPC requests concurrently)            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Interaction

```
┌──────────┐      HTTP        ┌──────────────────┐      gRPC       ┌─────────────────┐
│  Client  │ ───────────────► │  REST Controller │ ──────────────► │  gRPC Service   │
│  (curl)  │                  │  (Port 8080)     │                 │  (Port 9090)    │
└──────────┘                  └──────────────────┘                 └─────────────────┘
                                      │                                    │
                                      │                                    │
                                      ▼                                    ▼
                              ┌──────────────────┐                ┌─────────────────┐
                              │  gRPC Client     │                │  Business Logic │
                              │  (Stub)          │                │  (Greeting)     │
                              └──────────────────┘                └─────────────────┘
```

---

## gRPC Communication Patterns

gRPC supports four communication patterns, all implemented in this project:

### 1. Unary RPC (Request-Response)

```
┌──────────┐                              ┌──────────┐
│  Client  │                              │  Server  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  ─────── HelloRequest ──────────────►   │
     │           {name: "John"}                │
     │                                         │
     │   ◄────── HelloResponse ─────────────   │
     │           {message: "Hello, John!"}     │
     │                                         │
     ▼                                         ▼
```

**Use Case**: Simple request/response like authentication, single data fetch.

```protobuf
rpc SayHello (HelloRequest) returns (HelloResponse);
```

---

### 2. Server Streaming RPC

```
┌──────────┐                              ┌──────────┐
│  Client  │                              │  Server  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  ─────── HelloRequest ──────────────►   │
     │           {name: "John"}                │
     │                                         │
     │   ◄────── HelloResponse[1] ──────────   │
     │           {message: "Hello, John!"}     │
     │                                         │
     │   ◄────── HelloResponse[2] ──────────   │
     │           {message: "Hola, John!"}      │
     │                                         │
     │   ◄────── HelloResponse[3] ──────────   │
     │           {message: "Bonjour, John!"}   │
     │                                         │
     │   ◄────── [Stream Complete] ──────────  │
     │                                         │
     ▼                                         ▼
```

**Use Case**: Real-time updates, large data downloads, notifications.

```protobuf
rpc SayHelloStream (HelloRequest) returns (stream HelloResponse);
```

---

### 3. Client Streaming RPC

```
┌──────────┐                              ┌──────────┐
│  Client  │                              │  Server  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  ─────── HelloRequest[1] ───────────►   │
     │           {name: "Alice"}               │
     │                                         │
     │  ─────── HelloRequest[2] ───────────►   │
     │           {name: "Bob"}                 │
     │                                         │
     │  ─────── HelloRequest[3] ───────────►   │
     │           {name: "Charlie"}             │
     │                                         │
     │  ─────── [Stream Complete] ──────────►  │
     │                                         │
     │   ◄────── HelloResponse ─────────────   │
     │           {message: "Hello to all:      │
     │            Alice, Bob, Charlie!"}       │
     │                                         │
     ▼                                         ▼
```

**Use Case**: File uploads, bulk data submission, aggregation.

```protobuf
rpc SayHelloToMany (stream HelloRequest) returns (HelloResponse);
```

---

### 4. Bidirectional Streaming RPC

```
┌──────────┐                              ┌──────────┐
│  Client  │                              │  Server  │
└────┬─────┘                              └────┬─────┘
     │                                         │
     │  ─────── HelloRequest[1] ───────────►   │
     │           {name: "Alice"}               │
     │                                         │
     │   ◄────── HelloResponse[1] ──────────   │
     │           {message: "Hello, Alice!"}    │
     │                                         │
     │  ─────── HelloRequest[2] ───────────►   │
     │           {name: "Bob"}                 │
     │                                         │
     │   ◄────── HelloResponse[2] ──────────   │
     │           {message: "Hello, Bob!"}      │
     │                                         │
     │  ─────── [Stream Complete] ──────────►  │
     │                                         │
     │   ◄────── [Stream Complete] ──────────  │
     │                                         │
     ▼                                         ▼
```

**Use Case**: Chat applications, real-time collaboration, gaming.

```protobuf
rpc SayHelloBidirectional (stream HelloRequest) returns (stream HelloResponse);
```

---

## Virtual Threads Architecture

### Traditional vs Virtual Threads

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Traditional Platform Threads                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐                         │
│  │ Thread  │  │ Thread  │  │ Thread  │  │ Thread  │  ... (limited ~10K)    │
│  │   1     │  │   2     │  │   3     │  │   4     │                         │
│  │  ~1MB   │  │  ~1MB   │  │  ~1MB   │  │  ~1MB   │  ← Heavy memory usage  │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘                         │
│       │            │            │            │                               │
│       ▼            ▼            ▼            ▼                               │
│  ┌─────────────────────────────────────────────────┐                        │
│  │              OS Kernel Scheduler                │  ← Expensive switches  │
│  └─────────────────────────────────────────────────┘                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    Java 21 Virtual Threads                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐┌───┐  ... (millions!)      │
│  │VT1││VT2││VT3││VT4││VT5││VT6││VT7││VT8││VT9││...│  ← ~few KB each       │
│  └─┬─┘└─┬─┘└─┬─┘└─┬─┘└─┬─┘└─┬─┘└─┬─┘└─┬─┘└─┬─┘└───┘                        │
│    │    │    │    │    │    │    │    │    │                                │
│    └────┴────┴────┼────┴────┴────┴────┼────┘                                │
│                   │                   │                                      │
│                   ▼                   ▼                                      │
│            ┌───────────┐       ┌───────────┐                                │
│            │  Carrier  │       │  Carrier  │   ← Few platform threads       │
│            │  Thread 1 │       │  Thread 2 │     (= CPU cores)              │
│            └─────┬─────┘       └─────┬─────┘                                │
│                  │                   │                                       │
│                  └─────────┬─────────┘                                       │
│                            ▼                                                 │
│              ┌──────────────────────────┐                                   │
│              │   JVM Virtual Thread     │  ← Cheap context switches        │
│              │       Scheduler          │                                   │
│              └──────────────────────────┘                                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Virtual Thread Blocking Behavior

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                Virtual Thread During Blocking I/O                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Time ──────────────────────────────────────────────────────────────────►   │
│                                                                              │
│  Virtual Thread 1:                                                          │
│  ┌────────────┐     ┌─────────────────────────┐     ┌────────────┐          │
│  │  Running   │────►│  Blocked (I/O wait)     │────►│  Running   │          │
│  │            │     │  UNMOUNTED from carrier │     │            │          │
│  └────────────┘     └─────────────────────────┘     └────────────┘          │
│        │                      │                           │                  │
│        ▼                      ▼                           ▼                  │
│  ┌──────────┐          ┌──────────┐               ┌──────────┐              │
│  │ Carrier  │          │ Carrier  │               │ Carrier  │              │
│  │ Thread   │          │ Thread   │               │ Thread   │              │
│  │ (busy)   │          │ (FREE!)  │               │ (busy)   │              │
│  └──────────┘          └──────────┘               └──────────┘              │
│                              │                                               │
│                              ▼                                               │
│                    ┌──────────────────┐                                     │
│                    │ Can run another  │                                     │
│                    │ virtual thread!  │                                     │
│                    └──────────────────┘                                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Configuration in This Project

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Virtual Thread Configuration                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     VirtualThreadConfig.java                           │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │                                                                        │ │
│  │  1. applicationTaskExecutor()                                          │ │
│  │     └── Spring @Async operations use virtual threads                   │ │
│  │                                                                        │ │
│  │  2. protocolHandlerVirtualThreadExecutorCustomizer()                   │ │
│  │     └── Tomcat HTTP requests use virtual threads                       │ │
│  │                                                                        │ │
│  │  3. grpcServerConfigurer()                                             │ │
│  │     └── gRPC requests use virtual threads                              │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     application.yml                                    │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │  spring:                                                               │ │
│  │    threads:                                                            │ │
│  │      virtual:                                                          │ │
│  │        enabled: true  ← Enables virtual threads globally               │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Component Details

### Project Structure

```
grpctest/
├── docs/
│   └── architecture.md          ← You are here
│
├── src/main/
│   ├── proto/
│   │   └── greeting.proto       ← gRPC service definition
│   │
│   ├── java/com/example/grpc/
│   │   ├── GrpcVirtualThreadsApplication.java
│   │   │
│   │   ├── config/
│   │   │   └── VirtualThreadConfig.java    ← Virtual thread setup
│   │   │
│   │   ├── service/
│   │   │   └── GreetingGrpcService.java    ← gRPC implementation
│   │   │
│   │   ├── client/
│   │   │   └── GreetingGrpcClient.java     ← gRPC client
│   │   │
│   │   └── controller/
│   │       ├── GreetingController.java     ← REST endpoints
│   │       └── HealthController.java       ← Health checks
│   │
│   └── resources/
│       └── application.yml      ← Configuration
│
└── src/test/
    └── java/com/example/grpc/
        └── GreetingGrpcServiceTest.java
```

### Class Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Class Diagram                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────┐                                        │
│  │  GrpcVirtualThreadsApplication  │                                        │
│  │  ─────────────────────────────  │                                        │
│  │  + main(String[])               │                                        │
│  └─────────────────────────────────┘                                        │
│                   │                                                          │
│                   │ @SpringBootApplication                                   │
│                   ▼                                                          │
│  ┌─────────────────────────────────┐                                        │
│  │      VirtualThreadConfig        │                                        │
│  │  ─────────────────────────────  │                                        │
│  │  + applicationTaskExecutor()    │                                        │
│  │  + protocolHandler...()         │                                        │
│  │  + grpcServerConfigurer()       │                                        │
│  └─────────────────────────────────┘                                        │
│                                                                              │
│  ┌─────────────────────────────────┐     ┌─────────────────────────────────┐│
│  │      GreetingController         │     │     GreetingGrpcService         ││
│  │  ─────────────────────────────  │     │  ─────────────────────────────  ││
│  │  - grpcClient                   │     │  + sayHello()                   ││
│  │  + sayHello()                   │────►│  + sayHelloStream()             ││
│  │  + sayHelloStream()             │     │  + sayHelloToMany()             ││
│  │  + sayHelloToMany()             │     │  + sayHelloBidirectional()      ││
│  │  + sayHelloBidirectional()      │     └─────────────────────────────────┘│
│  └─────────────────────────────────┘                                        │
│                   │                              ▲                           │
│                   │ uses                         │ extends                   │
│                   ▼                              │                           │
│  ┌─────────────────────────────────┐     ┌─────────────────────────────────┐│
│  │      GreetingGrpcClient         │     │  GreetingServiceGrpc.           ││
│  │  ─────────────────────────────  │     │  GreetingServiceImplBase        ││
│  │  - blockingStub                 │     │  (Generated from .proto)        ││
│  │  - asyncStub                    │     └─────────────────────────────────┘│
│  │  + sayHello()                   │                                        │
│  │  + sayHelloStream()             │                                        │
│  │  + sayHelloToMany()             │                                        │
│  │  + sayHelloBidirectional()      │                                        │
│  └─────────────────────────────────┘                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### REST to gRPC Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Request Flow: REST → gRPC                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. HTTP Request                                                            │
│     GET /api/greetings/hello?name=John&language=en                          │
│                                                                              │
│     ┌──────────┐                                                            │
│     │  Client  │                                                            │
│     └────┬─────┘                                                            │
│          │                                                                   │
│          │ HTTP/1.1                                                          │
│          ▼                                                                   │
│  2. ┌────────────────────────────────────────┐                              │
│     │          Tomcat (Port 8080)            │                              │
│     │     [Virtual Thread Executor]          │                              │
│     └────────────────┬───────────────────────┘                              │
│                      │                                                       │
│                      ▼                                                       │
│  3. ┌────────────────────────────────────────┐                              │
│     │        GreetingController              │                              │
│     │     @GetMapping("/hello")              │                              │
│     └────────────────┬───────────────────────┘                              │
│                      │                                                       │
│                      ▼                                                       │
│  4. ┌────────────────────────────────────────┐                              │
│     │        GreetingGrpcClient              │                              │
│     │     blockingStub.sayHello()            │                              │
│     └────────────────┬───────────────────────┘                              │
│                      │                                                       │
│                      │ gRPC (HTTP/2)                                         │
│                      ▼                                                       │
│  5. ┌────────────────────────────────────────┐                              │
│     │        gRPC Server (Port 9090)         │                              │
│     │     [Virtual Thread Executor]          │                              │
│     └────────────────┬───────────────────────┘                              │
│                      │                                                       │
│                      ▼                                                       │
│  6. ┌────────────────────────────────────────┐                              │
│     │        GreetingGrpcService             │                              │
│     │     sayHello(HelloRequest)             │                              │
│     └────────────────┬───────────────────────┘                              │
│                      │                                                       │
│                      │ Business Logic                                        │
│                      ▼                                                       │
│  7. ┌────────────────────────────────────────┐                              │
│     │        HelloResponse                   │                              │
│     │     {message, timestamp, threadInfo}   │                              │
│     └────────────────────────────────────────┘                              │
│                                                                              │
│  Response flows back through the same path                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Protocol Buffer Serialization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Protocol Buffer Serialization                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Java Object                    Binary Wire Format                          │
│  ────────────                   ─────────────────                           │
│                                                                              │
│  HelloRequest {                 ┌─────────────────────────────────────────┐ │
│    name: "John"       ─────►    │ 0A 04 4A 6F 68 6E 12 02 65 6E           │ │
│    language: "en"               │ (compact binary, ~10 bytes)             │ │
│  }                              └─────────────────────────────────────────┘ │
│                                                                              │
│  vs JSON                        ┌─────────────────────────────────────────┐ │
│                                 │ {"name":"John","language":"en"}         │ │
│                                 │ (text, ~35 bytes)                       │ │
│                                 └─────────────────────────────────────────┘ │
│                                                                              │
│  Benefits:                                                                  │
│  • 3-10x smaller than JSON                                                  │
│  • Faster serialization/deserialization                                     │
│  • Strong typing with code generation                                       │
│  • Schema evolution support                                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## AWS Deployment Architecture

### Production Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AWS Production Architecture                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                              ┌─────────────────┐                            │
│                              │   CloudFront    │                            │
│                              │   (CDN/Cache)   │                            │
│                              └────────┬────────┘                            │
│                                       │                                      │
│                                       ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                            AWS VPC                                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐    │    │
│  │  │                      Public Subnets                          │    │    │
│  │  │                                                              │    │    │
│  │  │  ┌──────────────────────────────────────────────────────┐   │    │    │
│  │  │  │              Public ALB (Internet-facing)             │   │    │    │
│  │  │  │                    HTTPS:443                          │   │    │    │
│  │  │  │        (Routes to REST API on port 8080)              │   │    │    │
│  │  │  └───────────────────────┬──────────────────────────────┘   │    │    │
│  │  │                          │                                   │    │    │
│  │  └──────────────────────────┼───────────────────────────────────┘    │    │
│  │                             │                                         │    │
│  │  ┌──────────────────────────┼───────────────────────────────────┐    │    │
│  │  │                  Private Subnets                              │    │    │
│  │  │                          │                                    │    │    │
│  │  │                          ▼                                    │    │    │
│  │  │  ┌────────────────────────────────────────────────────────┐  │    │    │
│  │  │  │                    ECS Fargate                          │  │    │    │
│  │  │  │  ┌──────────────────────────────────────────────────┐  │  │    │    │
│  │  │  │  │              Spring Boot App                      │  │  │    │    │
│  │  │  │  │  ┌─────────────────┐  ┌─────────────────────┐    │  │  │    │    │
│  │  │  │  │  │  REST :8080     │  │   gRPC :9090        │    │  │  │    │    │
│  │  │  │  │  └─────────────────┘  └──────────▲──────────┘    │  │  │    │    │
│  │  │  │  └──────────────────────────────────┼───────────────┘  │  │    │    │
│  │  │  │  ┌──────────────────────────────────┼───────────────┐  │  │    │    │
│  │  │  │  │              Spring Boot App     │               │  │  │    │    │
│  │  │  │  │  ┌─────────────────┐  ┌─────────┴───────────┐    │  │  │    │    │
│  │  │  │  │  │  REST :8080     │  │   gRPC :9090        │    │  │  │    │    │
│  │  │  │  │  └─────────────────┘  └─────────────────────┘    │  │  │    │    │
│  │  │  │  └──────────────────────────────────────────────────┘  │  │    │    │
│  │  │  └────────────────────────────────────────────────────────┘  │    │    │
│  │  │                          ▲                                    │    │    │
│  │  │                          │                                    │    │    │
│  │  │  ┌───────────────────────┴────────────────────────────────┐  │    │    │
│  │  │  │           Internal ALB (gRPC - VPC only)                │  │    │    │
│  │  │  │                    HTTPS:443                            │  │    │    │
│  │  │  │         (Routes to gRPC on port 9090)                   │  │    │    │
│  │  │  └───────────────────────▲────────────────────────────────┘  │    │    │
│  │  │                          │                                    │    │    │
│  │  │                          │ gRPC calls                         │    │    │
│  │  │  ┌───────────────────────┴────────────────────────────────┐  │    │    │
│  │  │  │              Other Microservices                        │  │    │    │
│  │  │  │       (Can call gRPC via Internal ALB)                  │  │    │    │
│  │  │  └────────────────────────────────────────────────────────┘  │    │    │
│  │  │                                                              │    │    │
│  │  └──────────────────────────────────────────────────────────────┘    │    │
│  │                                                                       │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Network Security

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Security Groups                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Public ALB Security Group                                             │ │
│  │  ─────────────────────────────                                         │ │
│  │  Inbound:  443 (HTTPS)  ← 0.0.0.0/0 (Internet)                        │ │
│  │  Outbound: 8080         → App Security Group                           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Internal ALB Security Group                                           │ │
│  │  ───────────────────────────────                                       │ │
│  │  Inbound:  443 (HTTPS)  ← VPC CIDR only                               │ │
│  │  Outbound: 9090         → App Security Group                           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Application Security Group                                            │ │
│  │  ──────────────────────────────                                        │ │
│  │  Inbound:  8080  ← Public ALB SG                                      │ │
│  │            9090  ← Internal ALB SG                                     │ │
│  │            8081  ← VPC CIDR (health checks)                           │ │
│  │  Outbound: All   → 0.0.0.0/0                                          │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Performance Characteristics

### Comparison Table

| Metric | Platform Threads | Virtual Threads | gRPC vs REST |
|--------|-----------------|-----------------|--------------|
| Memory/Thread | ~1MB | ~few KB | N/A |
| Max Concurrent | ~10,000 | Millions | N/A |
| Context Switch | Expensive (OS) | Cheap (JVM) | N/A |
| Serialization | N/A | N/A | 3-10x smaller |
| Latency | N/A | N/A | Lower (HTTP/2) |
| Multiplexing | N/A | N/A | Yes (HTTP/2) |

---

## Quick Reference

### Endpoints

| Endpoint | Method | Port | Description |
|----------|--------|------|-------------|
| `/api/greetings/hello` | GET | 8080 | Unary greeting |
| `/api/greetings/hello-stream` | GET | 8080 | Server streaming |
| `/api/greetings/hello-many` | POST | 8080 | Client streaming |
| `/api/greetings/hello-bidirectional` | POST | 8080 | Bidirectional |
| `/health` | GET | 8080 | Health check |
| `GreetingService/*` | gRPC | 9090 | All gRPC methods |

### Key Configuration

```yaml
# Virtual threads
spring.threads.virtual.enabled: true

# Ports
server.port: 8080          # REST
grpc.server.port: 9090     # gRPC
management.server.port: 8081  # Health
```

