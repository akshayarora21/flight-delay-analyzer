FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
RUN mkdir -p src/main/resources
RUN chown -R appuser:appgroup /app
USER appuser
COPY --from=builder /app/target/flight-delay-analyzer-*.jar app.jar
VOLUME ["/app/src/main/resources"]
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]