# Backend Test Rehberi

## 1. Veritabanı Kurulumu
```sql
-- PostgreSQL'de çalıştırın:
\i database-setup.sql
```

## 2. Backend Çalıştırma
```bash
# Backend klasörüne gidin
cd market-supplier-backend

# Maven ile derleyin
mvn clean compile

# Uygulamayı çalıştırın
mvn spring-boot:run
```

## 3. Test Endpoint'leri

### Kayıt Ol
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Market",
    "email": "market@test.com",
    "password": "123456",
    "role": "MARKET"
  }'
```

### Giriş Yap
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "market@test.com",
    "password": "123456"
  }'
```

### Market Oluştur (Token gerekli)
```bash
curl -X POST http://localhost:8080/api/markets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Test Market",
    "address": "Test Adres",
    "phone": "05551234567"
  }'
```

## 4. Swagger UI
Uygulama çalıştıktan sonra:
- http://localhost:8080/swagger-ui.html

## 5. H2 Console (Development)
- http://localhost:8080/h2-console
- JDBC URL: jdbc:postgresql://localhost:5432/market_supplier_db
- Username: postgres
- Password: password
