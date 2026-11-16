# syntax=docker/dockerfile:1
# Imagem única para os 3 serviços do Catálogo. O serviço é escolhido pelo build arg SERVICE:
#   restaurantes-service | cardapio-service | busca-service
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build

COPY pom.xml .
COPY catalogo-common/pom.xml catalogo-common/
COPY restaurantes-service/pom.xml restaurantes-service/
COPY cardapio-service/pom.xml cardapio-service/
COPY busca-service/pom.xml busca-service/
COPY catalogo-common/src catalogo-common/src
COPY ${SERVICE}/src ${SERVICE}/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${SERVICE} -am -Dmaven.test.skip=true package \
    && cp ${SERVICE}/target/${SERVICE}.jar /app.jar

FROM eclipse-temurin:21-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app.jar app.jar

USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
