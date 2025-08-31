# Usa immagine base con OpenJDK
FROM eclipse-temurin:17-jdk-jammy

# Aggiorna i pacchetti per ridurre vulnerabilità
RUN apt-get update && apt-get upgrade -y && apt-get clean && rm -rf /var/lib/apt/lists/*

# Setta working directory all'interno del container
WORKDIR /app

# Copia il jar generato da sbt
COPY target/scala-3.3.6/agar-io-assembly-0.1.0-SNAPSHOT.jar /app/agario.jar

EXPOSE 19000

# Comando di default (porta e ruolo saranno passati da docker-compose)
ENTRYPOINT ["java","-jar","/app/agario.jar"]
