## build stage ##
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests=true

## run stage ##
FROM openjdk:17-alpine

RUN adduser -D coms-chinhbean

WORKDIR /run

COPY --from=build /app/target/history-education-0.0.1-SNAPSHOT.jar /run/history-education-0.0.1-SNAPSHOT.jar

# Correct chown command: Use the default group (history-edu)
RUN chown -R coms-chinhbean:coms-chinhbean /run

USER coms-chinhbean

EXPOSE 8080







ENTRYPOINT java -jar /run/history-education-0.0.1-SNAPSHOT.jar





