# -------- STAGE 1: Build with Maven --------
FROM maven:3.9.10-amazoncorretto-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

# -------- STAGE 2: Runtime container --------
FROM amazoncorretto:21
WORKDIR /app

COPY --from=builder /app/target/balance-streaming-system-*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]