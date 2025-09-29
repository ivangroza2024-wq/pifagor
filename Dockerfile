# Base image
FROM openjdk:17-jdk-slim

# Work directory
WORKDIR /app

# Copy project
COPY . .

# Build project
RUN ./mvnw clean package

# Run Spring Boot
CMD ["java", "-jar", "target/line-school-bot-backend-0.0.1-SNAPSHOT.jar"]