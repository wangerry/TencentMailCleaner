FROM openjdk:8-alpine
WORKDIR /app
COPY target/mailcleaner-0.0.1-SNAPSHOT.jar /app/main.jar
CMD java -jar main.jar
