-- PostgreSQL Database Setup for Market-Supplier System
-- Bu dosyayı PostgreSQL'de çalıştırarak veritabanını oluşturun

-- Veritabanı oluştur
CREATE DATABASE market_supplier_db;

-- Kullanıcı oluştur (isteğe bağlı)
CREATE USER market_user WITH PASSWORD 'market_password';

-- Veritabanı yetkilerini ver
GRANT ALL PRIVILEGES ON DATABASE market_supplier_db TO market_user;

-- Veritabanına bağlan
\c market_supplier_db;

-- Tablolar Spring Boot tarafından otomatik oluşturulacak
-- application.properties'te spring.jpa.hibernate.ddl-auto=update olduğu için
