FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/image-hotspot-demo-*.jar /app/app.jar
RUN mkdir -p /app/data && chown -R 1001:1001 /app
USER 1001
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
