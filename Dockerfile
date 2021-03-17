FROM openjdk:14.0.2-jdk-slim as build
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN ./gradlew build

FROM ghcr.io/graalvm/graalvm-ce:21.0.0.2
WORKDIR /app
COPY --from=build /usr/src/app/build/libs/*.jar .
CMD java -jar *.jar
