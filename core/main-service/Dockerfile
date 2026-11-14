FROM eclipse-temurin:21-jdk-alpine
LABEL authors="dima"
ENV APP_HOME=/app
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]