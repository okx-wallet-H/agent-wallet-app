# Agent Wallet Backend
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the fat jar
COPY build/libs/agent-wallet-backend-0.1.0-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
