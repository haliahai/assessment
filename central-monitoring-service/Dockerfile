FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/central-monitoring-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
