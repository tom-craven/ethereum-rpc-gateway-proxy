#todo: should have seperate build and run phases
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# todo: should explode jar
COPY build/libs/gateway-1.0.0-SNAPSHOT-fat.jar /app/app.jar
# todo: should get keystore from secrets store
COPY keystore.jks /app/keystore.jks
EXPOSE 443
# todo: should not run as root user
CMD ["java", "-jar", "/app/app.jar"]
