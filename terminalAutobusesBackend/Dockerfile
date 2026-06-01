# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy only pom.xml first to cache dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src src
RUN mvn package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install timezone data
RUN apk add --no-cache tzdata
ENV TZ=America/Caracas

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
