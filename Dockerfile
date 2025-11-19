# Copyright (C) 2023-2025 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Multi-stage build for Phive gRPC Service

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the project files
COPY pom.xml .
COPY src ./src
COPY protocol ./protocol

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/target/phive-grpc-service-*.jar /app/phive-service.jar

# Expose ports
# 9090 - gRPC server for validation requests
EXPOSE 9090

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/phive-service.jar"]
