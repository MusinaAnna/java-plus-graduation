FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-aggregator-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
