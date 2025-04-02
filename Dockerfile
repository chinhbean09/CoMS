        FROM maven:3.8.5-openjdk-17-slim AS build
        WORKDIR /app

        COPY pom.xml .
        RUN mvn dependency:go-offline -B

        COPY src ./src

        RUN mvn clean install -DskipTests=true

        FROM openjdk:17-alpine

        RUN apk add --no-cache tzdata && \
            ln -snf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
            echo "Asia/Ho_Chi_Minh" > /etc/timezone

        RUN adduser -D coms-chinhbean

        WORKDIR /run

        COPY --from=build /app/target/contract-management-0.0.1-SNAPSHOT.jar /run/contract-management-0.0.1-SNAPSHOT.jar

        RUN chown -R coms-chinhbean:coms-chinhbean /run

        USER coms-chinhbean

        EXPOSE 8088

        ENTRYPOINT ["java", "-jar", "/run/contract-management-0.0.1-SNAPSHOT.jar"]


