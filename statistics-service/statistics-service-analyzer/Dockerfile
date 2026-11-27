FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-analyzer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
