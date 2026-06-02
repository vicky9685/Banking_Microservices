# Multi-stage Dockerfile reused by every Spring Boot service.
# Build with:   docker build --build-arg SERVICE=account-service -t bank/account-service .
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY customer-service/pom.xml customer-service/
COPY account-service/pom.xml account-service/
COPY transaction-service/pom.xml transaction-service/
COPY notification-service/pom.xml notification-service/
COPY workflow-service/pom.xml workflow-service/
# Warm the local maven repo (best-effort) then bring in sources
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY common-lib/src common-lib/src
COPY config-server/src config-server/src
COPY discovery-server/src discovery-server/src
COPY api-gateway/src api-gateway/src
COPY auth-service/src auth-service/src
COPY customer-service/src customer-service/src
COPY account-service/src account-service/src
COPY transaction-service/src transaction-service/src
COPY notification-service/src notification-service/src
COPY workflow-service/src workflow-service/src
ARG SERVICE
RUN mvn -B -DskipTests -pl ${SERVICE} -am package

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
ENV SERVICE_NAME=${SERVICE}
RUN addgroup -S bank && adduser -S bank -G bank
WORKDIR /app
COPY --from=builder /workspace/${SERVICE}/target/*.jar /app/app.jar
USER bank
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
