FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8088
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=25.0 -XX:MaxMetaspaceSize=160m -Dfile.encoding=UTF-8"
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8088
CMD ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
