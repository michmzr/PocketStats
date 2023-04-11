#Build stage
FROM gradle:latest AS BUILD
WORKDIR ./
COPY build.gradle  ./
COPY src src
RUN gradle build  --no-daemon

# Package stage
FROM openjdk:19
VOLUME /tmp
EXPOSE 8081
ARG JAR_FILE=build/libs/backend-2.1-all.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]