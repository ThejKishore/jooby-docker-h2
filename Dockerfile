FROM gradle:8-jdk21-alpine as build
WORKDIR /myapp-java
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY src src
COPY conf conf
RUN gradle shadowJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /myapp-java
COPY --from=build /myapp-java/build/libs/myapp-java-1.0.0-all.jar app.jar
COPY conf conf
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
