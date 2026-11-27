FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-collector-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
