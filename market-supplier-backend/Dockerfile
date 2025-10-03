# Spring Boot uygulaması için temel imaj
FROM openjdk:17-jdk-slim

# Çalışma dizini
WORKDIR /app

# Maven kullanıyorsan jar dosyasını kopyala
COPY target/*.jar app.jar

# Uygulamayı başlat
ENTRYPOINT ["java","-jar","/app/app.jar"]