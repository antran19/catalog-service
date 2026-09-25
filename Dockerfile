FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY libs libs
COPY platform platform
COPY services services
RUN mvn -q -pl services/catalog-service -am -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build /workspace/services/catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
