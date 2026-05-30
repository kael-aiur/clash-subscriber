# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY module-common/pom.xml module-common/
COPY module-subscription/pom.xml module-subscription/
COPY module-processor/pom.xml module-processor/
COPY module-mihomo/pom.xml module-mihomo/
COPY module-scheduler/pom.xml module-scheduler/
COPY module-pipeline/pom.xml module-pipeline/
COPY module-web/pom.xml module-web/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/module-web/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
