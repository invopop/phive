/*
 * Copyright (C) 2025 Invopop Ltd. (https://invopop.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.PhiveRulesInitializer;
import rpc.ValidationServiceImpl;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Main gRPC server for Phive validation service.
 * Plain Java application without Spring Boot framework.
 *
 * @author Invopop Ltd.
 */
public class PhiveGrpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhiveGrpcServer.class);

    private Server server;
    private final int port;

    public PhiveGrpcServer(final int port) {
        this.port = port;
    }

    public void start() throws IOException {
        LOGGER.info("Initializing Phive gRPC Server on port {}", port);

        // Initialize phive rules
        final PhiveRulesInitializer rulesInitializer = new PhiveRulesInitializer();
        rulesInitializer.initialize();

        // Create validation service
        final ValidationServiceImpl validationService = new ValidationServiceImpl(rulesInitializer);

        // Build and start gRPC server
        server = ServerBuilder.forPort(port)
                .addService(validationService)
                .build()
                .start();

        LOGGER.info("Phive gRPC Server started successfully on port {}", port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down gRPC server (JVM shutdown)");
            PhiveGrpcServer.this.stop();
        }));
    }

    public void stop() {
        if (server != null) {
            LOGGER.info("Stopping gRPC server");
            server.shutdown();
            try {
                server.awaitTermination();
                LOGGER.info("gRPC server stopped successfully");
            } catch (final InterruptedException e) {
                LOGGER.error("Error while waiting for gRPC server termination", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(final String[] args) throws IOException, InterruptedException {
        // Get port from environment variable or use default
        final String portStr = System.getenv("GRPC_SERVER_PORT");
        final int port = portStr != null ? Integer.parseInt(portStr) : 9090;

        final PhiveGrpcServer server = new PhiveGrpcServer(port);
        server.start();
        server.blockUntilShutdown();
    }
}
