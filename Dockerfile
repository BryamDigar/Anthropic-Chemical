# Fase de construcción (Build stage)
FROM eclipse-temurin:21-jdk AS build

# Instalar dependencias necesarias para la compilación
RUN apt-get update && apt-get install -y \
    git \
    wget

WORKDIR /app
COPY . .

RUN ls -la /app

# Convertir gradlew a formato Unix, asignarle permisos y ejecutar el build
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon

# Fase de ejecución (Run stage)
FROM eclipse-temurin:21-jdk

EXPOSE 8080

# Correct the path to match the output directory of the bootJar task
COPY --from=build /app/build/libs/email-0.0.1-SNAPSHOT.jar email.jar

ENTRYPOINT ["java", "-jar", "email.jar"]
