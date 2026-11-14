FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -u 10001 -m ranger
USER ranger
COPY --from=build /workspace/target/backend-0.1.0.jar /app/ranger.jar
ENV RANGER_REQUIRE_PERSISTENT_DB=true
ENV BIND_ADDRESS=0.0.0.0
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:ActiveProcessorCount=1"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/ranger.jar"]
