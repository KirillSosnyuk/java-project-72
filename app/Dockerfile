FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon build
EXPOSE 7070
CMD ["./gradlew", "--no-daemon", "run"]