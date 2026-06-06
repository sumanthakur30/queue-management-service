FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY security-common ./security-common
RUN mvn -f security-common/pom.xml -B -DskipTests install

COPY queue-management-service ./queue-management-service
RUN mvn -f queue-management-service/pom.xml -B -DskipTests package && cp /workspace/queue-management-service/target/*-SNAPSHOT.jar /workspace/queue-management-service/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/queue-management-service/app.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java", "-jar", "app.jar"]
