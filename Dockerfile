# Base image
FROM openjdk:17-jdk-slim

# Work directory
WORKDIR /app

# Copy project
COPY . .
# Дати права на виконання скрипту mvnw
RUN chmod +x ./mvnw
# Build project
RUN ./mvnw clean package

# Run Spring Boot
CMD ["java", "-jar", "target/pifagor-0.0.1-SNAPSHOT.jar"]