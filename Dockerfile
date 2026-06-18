FROM sugamflow-common-libs:local AS build
WORKDIR /workspace

COPY docker/maven-docker-settings.xml /root/.m2/settings.xml
COPY docker/mvn-package-retry.sh /usr/local/bin/mvn-package-retry.sh
COPY queue-management-service ./queue-management-service
RUN sed -i 's/\r$//' /usr/local/bin/mvn-package-retry.sh \
    && chmod +x /usr/local/bin/mvn-package-retry.sh \
    && sh /usr/local/bin/mvn-package-retry.sh queue-management-service/pom.xml \
    && cp /workspace/queue-management-service/target/*-SNAPSHOT.jar /workspace/queue-management-service/app.jar

FROM sugamflow-jre:local
WORKDIR /app
COPY --from=build /workspace/queue-management-service/app.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java", "-jar", "app.jar"]