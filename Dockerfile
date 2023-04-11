FROM openjdk:19
VOLUME /tmp
EXPOSE 8081
ARG JAR_FILE=backend/build/libs/backend-2.1-all.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]