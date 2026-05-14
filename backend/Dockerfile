FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Default for containerized runtime; override with -e MONGODB_URI=... when needed.
ENV MONGODB_URI=mongodb://mongo:27017/earthquake_dashboard

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
