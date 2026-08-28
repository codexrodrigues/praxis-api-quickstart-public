FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY db ./db
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8088
# Let the container memory limit govern metaspace instead of imposing a second,
# undersized ceiling. Exit-on-OOM lets Render replace a failed process promptly
# instead of leaving an HTTP port backed by a degraded JVM.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
COPY --from=build /app/target/*.jar /app/app.jar
COPY --chmod=0755 scripts/workspace/Invoke-OperationalDatasourceMigrations.sh /app/bin/praxis-operational-migrate
EXPOSE 8088
CMD ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
