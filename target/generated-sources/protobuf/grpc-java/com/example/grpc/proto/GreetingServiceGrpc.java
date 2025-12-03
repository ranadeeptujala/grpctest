package com.example.grpc.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * gRPC Service with all 4 communication patterns
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.0)",
    comments = "Source: greeting.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GreetingServiceGrpc {

  private GreetingServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "greeting.GreetingService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayHello",
      requestType = com.example.grpc.proto.HelloRequest.class,
      responseType = com.example.grpc.proto.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloMethod() {
    io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse> getSayHelloMethod;
    if ((getSayHelloMethod = GreetingServiceGrpc.getSayHelloMethod) == null) {
      synchronized (GreetingServiceGrpc.class) {
        if ((getSayHelloMethod = GreetingServiceGrpc.getSayHelloMethod) == null) {
          GreetingServiceGrpc.getSayHelloMethod = getSayHelloMethod =
              io.grpc.MethodDescriptor.<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("SayHello"))
              .build();
        }
      }
    }
    return getSayHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloServerStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayHelloServerStream",
      requestType = com.example.grpc.proto.HelloRequest.class,
      responseType = com.example.grpc.proto.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloServerStreamMethod() {
    io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse> getSayHelloServerStreamMethod;
    if ((getSayHelloServerStreamMethod = GreetingServiceGrpc.getSayHelloServerStreamMethod) == null) {
      synchronized (GreetingServiceGrpc.class) {
        if ((getSayHelloServerStreamMethod = GreetingServiceGrpc.getSayHelloServerStreamMethod) == null) {
          GreetingServiceGrpc.getSayHelloServerStreamMethod = getSayHelloServerStreamMethod =
              io.grpc.MethodDescriptor.<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHelloServerStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("SayHelloServerStream"))
              .build();
        }
      }
    }
    return getSayHelloServerStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloClientStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayHelloClientStream",
      requestType = com.example.grpc.proto.HelloRequest.class,
      responseType = com.example.grpc.proto.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloClientStreamMethod() {
    io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse> getSayHelloClientStreamMethod;
    if ((getSayHelloClientStreamMethod = GreetingServiceGrpc.getSayHelloClientStreamMethod) == null) {
      synchronized (GreetingServiceGrpc.class) {
        if ((getSayHelloClientStreamMethod = GreetingServiceGrpc.getSayHelloClientStreamMethod) == null) {
          GreetingServiceGrpc.getSayHelloClientStreamMethod = getSayHelloClientStreamMethod =
              io.grpc.MethodDescriptor.<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHelloClientStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("SayHelloClientStream"))
              .build();
        }
      }
    }
    return getSayHelloClientStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloBidirectionalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SayHelloBidirectional",
      requestType = com.example.grpc.proto.HelloRequest.class,
      responseType = com.example.grpc.proto.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest,
      com.example.grpc.proto.HelloResponse> getSayHelloBidirectionalMethod() {
    io.grpc.MethodDescriptor<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse> getSayHelloBidirectionalMethod;
    if ((getSayHelloBidirectionalMethod = GreetingServiceGrpc.getSayHelloBidirectionalMethod) == null) {
      synchronized (GreetingServiceGrpc.class) {
        if ((getSayHelloBidirectionalMethod = GreetingServiceGrpc.getSayHelloBidirectionalMethod) == null) {
          GreetingServiceGrpc.getSayHelloBidirectionalMethod = getSayHelloBidirectionalMethod =
              io.grpc.MethodDescriptor.<com.example.grpc.proto.HelloRequest, com.example.grpc.proto.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SayHelloBidirectional"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.example.grpc.proto.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GreetingServiceMethodDescriptorSupplier("SayHelloBidirectional"))
              .build();
        }
      }
    }
    return getSayHelloBidirectionalMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GreetingServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreetingServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceStub>() {
        @java.lang.Override
        public GreetingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreetingServiceStub(channel, callOptions);
        }
      };
    return GreetingServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GreetingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreetingServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceBlockingStub>() {
        @java.lang.Override
        public GreetingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreetingServiceBlockingStub(channel, callOptions);
        }
      };
    return GreetingServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GreetingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GreetingServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GreetingServiceFutureStub>() {
        @java.lang.Override
        public GreetingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GreetingServiceFutureStub(channel, callOptions);
        }
      };
    return GreetingServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * gRPC Service with all 4 communication patterns
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 1. Unary RPC - Single request/response
     * </pre>
     */
    default void sayHello(com.example.grpc.proto.HelloRequest request,
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHelloMethod(), responseObserver);
    }

    /**
     * <pre>
     * 2. Server Streaming RPC - Single request, stream of responses
     * </pre>
     */
    default void sayHelloServerStream(com.example.grpc.proto.HelloRequest request,
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSayHelloServerStreamMethod(), responseObserver);
    }

    /**
     * <pre>
     * 3. Client Streaming RPC - Stream of requests, single response
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloRequest> sayHelloClientStream(
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSayHelloClientStreamMethod(), responseObserver);
    }

    /**
     * <pre>
     * 4. Bidirectional Streaming RPC - Stream both ways
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloRequest> sayHelloBidirectional(
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSayHelloBidirectionalMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service GreetingService.
   * <pre>
   * gRPC Service with all 4 communication patterns
   * </pre>
   */
  public static abstract class GreetingServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return GreetingServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service GreetingService.
   * <pre>
   * gRPC Service with all 4 communication patterns
   * </pre>
   */
  public static final class GreetingServiceStub
      extends io.grpc.stub.AbstractAsyncStub<GreetingServiceStub> {
    private GreetingServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreetingServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreetingServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. Unary RPC - Single request/response
     * </pre>
     */
    public void sayHello(com.example.grpc.proto.HelloRequest request,
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSayHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 2. Server Streaming RPC - Single request, stream of responses
     * </pre>
     */
    public void sayHelloServerStream(com.example.grpc.proto.HelloRequest request,
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSayHelloServerStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 3. Client Streaming RPC - Stream of requests, single response
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloRequest> sayHelloClientStream(
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSayHelloClientStreamMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * 4. Bidirectional Streaming RPC - Stream both ways
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloRequest> sayHelloBidirectional(
        io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getSayHelloBidirectionalMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service GreetingService.
   * <pre>
   * gRPC Service with all 4 communication patterns
   * </pre>
   */
  public static final class GreetingServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<GreetingServiceBlockingStub> {
    private GreetingServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreetingServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreetingServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. Unary RPC - Single request/response
     * </pre>
     */
    public com.example.grpc.proto.HelloResponse sayHello(com.example.grpc.proto.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSayHelloMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 2. Server Streaming RPC - Single request, stream of responses
     * </pre>
     */
    public java.util.Iterator<com.example.grpc.proto.HelloResponse> sayHelloServerStream(
        com.example.grpc.proto.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSayHelloServerStreamMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service GreetingService.
   * <pre>
   * gRPC Service with all 4 communication patterns
   * </pre>
   */
  public static final class GreetingServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<GreetingServiceFutureStub> {
    private GreetingServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GreetingServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GreetingServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 1. Unary RPC - Single request/response
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.grpc.proto.HelloResponse> sayHello(
        com.example.grpc.proto.HelloRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSayHelloMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SAY_HELLO = 0;
  private static final int METHODID_SAY_HELLO_SERVER_STREAM = 1;
  private static final int METHODID_SAY_HELLO_CLIENT_STREAM = 2;
  private static final int METHODID_SAY_HELLO_BIDIRECTIONAL = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAY_HELLO:
          serviceImpl.sayHello((com.example.grpc.proto.HelloRequest) request,
              (io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse>) responseObserver);
          break;
        case METHODID_SAY_HELLO_SERVER_STREAM:
          serviceImpl.sayHelloServerStream((com.example.grpc.proto.HelloRequest) request,
              (io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SAY_HELLO_CLIENT_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sayHelloClientStream(
              (io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse>) responseObserver);
        case METHODID_SAY_HELLO_BIDIRECTIONAL:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.sayHelloBidirectional(
              (io.grpc.stub.StreamObserver<com.example.grpc.proto.HelloResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSayHelloMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.grpc.proto.HelloRequest,
              com.example.grpc.proto.HelloResponse>(
                service, METHODID_SAY_HELLO)))
        .addMethod(
          getSayHelloServerStreamMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.example.grpc.proto.HelloRequest,
              com.example.grpc.proto.HelloResponse>(
                service, METHODID_SAY_HELLO_SERVER_STREAM)))
        .addMethod(
          getSayHelloClientStreamMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              com.example.grpc.proto.HelloRequest,
              com.example.grpc.proto.HelloResponse>(
                service, METHODID_SAY_HELLO_CLIENT_STREAM)))
        .addMethod(
          getSayHelloBidirectionalMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.example.grpc.proto.HelloRequest,
              com.example.grpc.proto.HelloResponse>(
                service, METHODID_SAY_HELLO_BIDIRECTIONAL)))
        .build();
  }

  private static abstract class GreetingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GreetingServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.example.grpc.proto.GreetingProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GreetingService");
    }
  }

  private static final class GreetingServiceFileDescriptorSupplier
      extends GreetingServiceBaseDescriptorSupplier {
    GreetingServiceFileDescriptorSupplier() {}
  }

  private static final class GreetingServiceMethodDescriptorSupplier
      extends GreetingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    GreetingServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GreetingServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GreetingServiceFileDescriptorSupplier())
              .addMethod(getSayHelloMethod())
              .addMethod(getSayHelloServerStreamMethod())
              .addMethod(getSayHelloClientStreamMethod())
              .addMethod(getSayHelloBidirectionalMethod())
              .build();
        }
      }
    }
    return result;
  }
}
