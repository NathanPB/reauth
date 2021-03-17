FROM ghcr.io/graalvm/graalvm-ce:java11-21.0.0.2
WORKDIR /app

COPY . /build
WORKDIR /build
RUN ./gradlew build

WORKDIR /app
RUN cp /build/build/libs/*.jar .
CMD java -jar *.jar
