# gRPC Server with Java 21 Virtual Threads

A high-performance gRPC server using **Java 21 Virtual Threads** and **Spring Boot 3.x**.

## Features

- ✅ **Java 21 Virtual Threads** - Handle millions of concurrent requests
- ✅ **gRPC** - High-performance RPC with Protocol Buffers
- ✅ **All 4 gRPC Patterns** - Unary, Server Streaming, Client Streaming, Bidirectional
- ✅ **Spring Boot 3.2** - Modern framework with native virtual thread support

## Architecture

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

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Run the Server

```bash
mvn clean compile spring-boot:run
```

### Ports

| Port | Service |
|------|---------|
| **9090** | gRPC Server |
| **8081** | Health Check (Actuator) |

## gRPC Methods

| Method | Pattern | Description |
|--------|---------|-------------|
| `SayHello` | Unary | Single request → Single response |
| `SayHelloServerStream` | Server Streaming | Single request → Stream of responses |
| `SayHelloClientStream` | Client Streaming | Stream of requests → Single response |
| `SayHelloBidirectional` | Bidirectional | Stream ↔ Stream |

## Test with grpcurl

```bash
# List services
grpcurl -plaintext localhost:9090 list

# Unary
grpcurl -plaintext -d '{"name":"John","language":"en"}' \
  localhost:9090 greeting.GreetingService/SayHello

# Server Streaming
grpcurl -plaintext -d '{"name":"John"}' \
  localhost:9090 greeting.GreetingService/SayHelloServerStream

# Client Streaming
grpcurl -plaintext -d '{"name":"Alice"}
{"name":"Bob"}
{"name":"Charlie"}' \
  localhost:9090 greeting.GreetingService/SayHelloClientStream

# Bidirectional
grpcurl -plaintext -d '{"name":"Alice"}
{"name":"Bob"}' \
  localhost:9090 greeting.GreetingService/SayHelloBidirectional
```

## Project Structure

```
src/main/
├── proto/
│   └── greeting.proto              # gRPC service definition
├── java/com/example/grpc/
│   ├── GrpcServerApplication.java  # Main application
│   ├── config/
│   │   └── VirtualThreadConfig.java # Virtual threads setup
│   └── service/
│       └── GreetingGrpcService.java # gRPC implementation
└── resources/
    └── application.yml             # Configuration
```

---

# How to Call This gRPC Server from Another Microservice

## Step 1: Copy Proto File

Copy `src/main/proto/greeting.proto` to your client microservice.

## Step 2: Add Dependencies

```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-client-spring-boot-starter</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.60.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.60.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.60.0</version>
</dependency>
```

## Step 3: Configure Client

```yaml
grpc:
  client:
    greeting-service:
      address: static://localhost:9090      # Local
      # address: dns:///grpc-alb.internal:443  # AWS
      negotiation-type: plaintext
```

## Step 4: Create gRPC Client

```java
@Service
public class GreetingGrpcClient {

    @GrpcClient("greeting-service")
    private GreetingServiceGrpc.GreetingServiceBlockingStub blockingStub;

    // Unary
    public HelloResponse sayHello(String name, String language) {
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .setLanguage(language)
                .build();
        return blockingStub.sayHello(request);
    }

    // Server Streaming
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
}
```

## Step 5: Use in Your Service

```java
@Service
public class OrderService {

    private final GreetingGrpcClient grpcClient;

    public OrderService(GreetingGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public void processOrder(String customerName) {
        HelloResponse greeting = grpcClient.sayHello(customerName, "en");
        System.out.println("Greeting: " + greeting.getMessage());
    }
}
```

---

## Virtual Threads

This server uses Java 21 Virtual Threads for maximum concurrency:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              Virtual Threads (Millions possible!)                            │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐            │
│  │VT1 │ │VT2 │ │VT3 │ │VT4 │ │VT5 │ │VT6 │ │VT7 │ │VT8 │ │... │            │
│  │gRPC│ │gRPC│ │gRPC│ │gRPC│ │gRPC│ │gRPC│ │gRPC│ │gRPC│ │gRPC│            │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘            │
│       └────────────────────┬────────────────────────┘                       │
│                            ▼                                                 │
│               ┌─────────┐ ┌─────────┐ ┌─────────┐                           │
│               │Carrier 1│ │Carrier 2│ │Carrier 3│  (Few platform threads)   │
│               └─────────┘ └─────────┘ └─────────┘                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

| Aspect | Platform Threads | Virtual Threads |
|--------|-----------------|-----------------|
| Memory | ~1MB each | ~few KB each |
| Max concurrent | ~10,000 | **Millions** |
| Blocking I/O | Blocks OS thread | **Unmounts** (free!) |

---

## AWS Deployment

### Internal ALB Configuration

```yaml
grpc:
  client:
    greeting-service:
      address: dns:///grpc-internal-alb.yourcompany.internal:443
      negotiation-type: tls
```

### Network Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS VPC                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         Private Subnet                                 │  │
│  │  ┌─────────────────┐         ┌─────────────────┐                      │  │
│  │  │ Order Service   │         │ User Service    │                      │  │
│  │  │ (gRPC Client)   │         │ (gRPC Client)   │                      │  │
│  │  └────────┬────────┘         └────────┬────────┘                      │  │
│  │           └───────────┬───────────────┘                                │  │
│  │                       ▼                                                │  │
│  │           ┌───────────────────────┐                                   │  │
│  │           │    Internal ALB       │                                   │  │
│  │           │    (gRPC - HTTP/2)    │                                   │  │
│  │           └───────────┬───────────┘                                   │  │
│  │                       ▼                                                │  │
│  │           ┌───────────────────────┐                                   │  │
│  │           │    gRPC Server        │                                   │  │
│  │           │    Port 9090          │                                   │  │
│  │           └───────────────────────┘                                   │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Documentation

See `docs/` folder for detailed documentation:
- `architecture.md` - System architecture
- `grpc-client-guide.md` - Full client implementation guide
- `aws-alb-config.md` - AWS ALB configuration
- `sequence-diagrams.md` - Sequence diagrams

## License

MIT
