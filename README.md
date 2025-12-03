# Java 21 Virtual Threads + Spring Boot + gRPC Demo

A demonstration project showcasing **Java 21 Virtual Threads** with **Spring Boot 3.x** and **gRPC**.

## Features

- ✅ **Java 21 Virtual Threads** - Lightweight concurrency for massive scalability
- ✅ **Spring Boot 3.2** - Modern Spring framework with native virtual thread support
- ✅ **gRPC** - High-performance RPC framework with all four communication patterns
- ✅ **Automatic Code Generation** - Proto files compiled to Java classes via Maven
- ✅ **REST API Bridge** - HTTP endpoints to test gRPC services

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
├─────────────────────────────────────────────────────────────┤
│  REST Controller (Port 8080)     │   gRPC Server (Port 9090) │
│  ┌─────────────────────────┐     │   ┌─────────────────────┐ │
│  │ /api/greetings/*        │     │   │ GreetingService     │ │
│  │ (Virtual Threads)       │────►│   │ (Virtual Threads)   │ │
│  └─────────────────────────┘     │   └─────────────────────┘ │
│              │                   │             ▲              │
│              ▼                   │             │              │
│  ┌─────────────────────────┐     │             │              │
│  │ gRPC Client             │─────┼─────────────┘              │
│  │ (Blocking & Async)      │     │                            │
│  └─────────────────────────┘     │                            │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+**

## Quick Start

### 1. Build the Project

```bash
# Clean and compile (generates gRPC classes from proto files)
mvn clean compile

# Or build the full package
mvn clean package -DskipTests
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

### 3. Test the Endpoints

#### Check Health (verify virtual threads are active)
```bash
curl http://localhost:8080/api/greetings/health
```

Expected response:
```json
{
  "status": "UP",
  "threadName": "tomcat-handler-0",
  "isVirtualThread": true,
  "threadId": 42,
  "javaVersion": "21.0.1",
  "availableProcessors": 8
}
```

#### Simple Greeting (Unary RPC)
```bash
curl "http://localhost:8080/api/greetings/hello?name=John&language=en"
```

#### Streaming Greetings (Server Streaming RPC)
```bash
curl "http://localhost:8080/api/greetings/hello-stream?name=John"
```

#### Multi-Greeting (Client Streaming RPC)
```bash
curl -X POST http://localhost:8080/api/greetings/hello-many \
  -H "Content-Type: application/json" \
  -d '["Alice", "Bob", "Charlie"]'
```

#### Bidirectional Streaming
```bash
curl -X POST http://localhost:8080/api/greetings/hello-bidirectional \
  -H "Content-Type: application/json" \
  -d '["Alice", "Bob", "Charlie"]'
```

### 4. Test with grpcurl (Optional)

If you have [grpcurl](https://github.com/fullstorydev/grpcurl) installed:

```bash
# List services
grpcurl -plaintext localhost:9090 list

# Call the service directly
grpcurl -plaintext -d '{"name": "World", "language": "en"}' \
  localhost:9090 greeting.GreetingService/SayHello
```

## Project Structure

```
grpctest/
├── pom.xml                              # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/example/grpc/
│   │   │   ├── GrpcVirtualThreadsApplication.java  # Main app
│   │   │   ├── config/
│   │   │   │   └── VirtualThreadConfig.java        # Virtual thread config
│   │   │   ├── service/
│   │   │   │   └── GreetingGrpcService.java        # gRPC service impl
│   │   │   ├── client/
│   │   │   │   └── GreetingGrpcClient.java         # gRPC client
│   │   │   └── controller/
│   │   │       └── GreetingController.java         # REST endpoints
│   │   ├── proto/
│   │   │   └── greeting.proto                      # Proto definitions
│   │   └── resources/
│   │       └── application.yml                     # Configuration
│   └── test/
│       └── java/com/example/grpc/
│           └── GreetingGrpcServiceTest.java        # Integration tests
└── README.md
```

## Virtual Threads Configuration

Virtual threads are enabled in three places:

### 1. Spring MVC (application.yml)
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 2. Tomcat HTTP Connector (VirtualThreadConfig.java)
```java
@Bean
public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
    return protocolHandler -> {
        protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    };
}
```

### 3. gRPC Server (VirtualThreadConfig.java)
```java
@Bean
public GrpcServerConfigurer grpcServerConfigurer() {
    return serverBuilder -> {
        serverBuilder.executor(Executors.newVirtualThreadPerTaskExecutor());
    };
}
```

## gRPC Communication Patterns

| Pattern | Method | Description |
|---------|--------|-------------|
| Unary | `SayHello` | Single request → Single response |
| Server Streaming | `SayHelloStream` | Single request → Stream of responses |
| Client Streaming | `SayHelloToMany` | Stream of requests → Single response |
| Bidirectional | `SayHelloBidirectional` | Stream of requests ↔ Stream of responses |

## Supported Languages

The greeting service supports multiple languages:

| Code | Language | Greeting |
|------|----------|----------|
| `en` | English | Hello |
| `es` | Spanish | Hola |
| `fr` | French | Bonjour |
| `de` | German | Hallo |
| `it` | Italian | Ciao |
| `pt` | Portuguese | Olá |
| `ja` | Japanese | こんにちは |
| `ko` | Korean | 안녕하세요 |
| `zh` | Chinese | 你好 |

## Running Tests

```bash
mvn test
```

## Why Virtual Threads?

Virtual threads (Project Loom) provide:

1. **Lightweight** - Millions of virtual threads vs thousands of platform threads
2. **Simple Code** - Write blocking code that scales like async
3. **Better Resource Utilization** - Virtual threads unmount from carrier threads during blocking
4. **Reduced Complexity** - No need for reactive/callback programming models

### Performance Comparison

| Metric | Platform Threads | Virtual Threads |
|--------|-----------------|-----------------|
| Memory per thread | ~1MB | ~few KB |
| Max concurrent | ~10,000 | ~millions |
| Context switch | OS kernel | JVM (cheap) |
| Blocking I/O | Blocks OS thread | Unmounts, reuses |

## Troubleshooting

### Proto compilation fails
```bash
# Ensure you have the os-maven-plugin extension
mvn clean compile -X
```

### Port already in use
```bash
# Check what's using the ports
lsof -i :8080
lsof -i :9090
```

### Virtual threads not working
Ensure you're running with Java 21+:
```bash
java -version
mvn -version
```

## License

MIT License
