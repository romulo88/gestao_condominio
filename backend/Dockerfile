# Build multi-stage: compila com Maven/JDK 21 e roda numa imagem só com JRE (bem menor).
# O JAR é gerado pelo spring-boot-maven-plugin (fat jar, tudo dentro).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa as dependências numa camada própria: enquanto o pom.xml não mudar, o Docker
# reaproveita essa camada e o build só recompila o código. `|| true` porque o
# dependency:go-offline falha em alguns plugins que só resolvem na hora do package -
# não é motivo pra quebrar o build, o `package` abaixo baixa o que faltar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Roda como usuário sem privilégio - se o processo for comprometido, não é root no container.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

# /v3/api-docs é permitAll no SecurityConfig, então serve de health check sem precisar
# adicionar o actuator só pra isso. start-period alto porque o Flyway roda as migrations
# antes da aplicação atender - num banco novo isso leva alguns segundos.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/v3/api-docs > /dev/null || exit 1

# MaxRAMPercentage em vez de -Xmx fixo: a JVM respeita o limite de memória do container,
# então dá pra apertar/afrouxar o serviço no compose sem mexer na imagem.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
