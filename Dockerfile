FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system mesh && adduser --system --ingroup mesh mesh
COPY --from=build /workspace/target/mesh-*.jar app.jar
USER mesh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
