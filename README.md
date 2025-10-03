📦 Market-Supplier System (MVP)

Bu proje, market sahipleri ile tedarikçileri buluşturan bir sipariş ve teslimat yönetim sistemidir.
Market sahipleri sipariş oluşturur, tedarikçiler siparişleri listeler, rota planlar ve teslimat raporları oluşturur.

🚀 Tech Stack

Backend: Spring Boot (Java, REST API)

Mobile App: Flutter

Database: PostgreSQL

Map & Route: Google Maps API veya OpenRouteService

PDF Raporlama: iText / Apache PDFBox

📂 Proje Yapısı
Backend (market-supplier-backend/)
market-supplier-backend/
├── src/main/java/com/example/marketsupplier/
│   ├── config/               # Güvenlik & DB config
│   ├── controller/           # REST API endpointleri
│   ├── dto/                  # Request/Response DTO’ları
│   ├── entity/               # JPA Entity’ler (User, Order vs.)
│   ├── repository/           # Spring Data JPA Repository’ler
│   ├── service/              # İş mantığı (Sipariş, Rapor vs.)
│   ├── util/                 # Yardımcı sınıflar (PDF, Rota)
│   └── MarketSupplierApplication.java
│
├── src/main/resources/
│   └── application.properties
│
├── pom.xml
└── README.md

Mobile App (market-supplier-app/)
market-supplier-app/
├── lib/
│   ├── api/        # Backend API servisleri
│   ├── models/     # Data modelleri
│   ├── providers/  # State management
│   ├── screens/    # Market & Tedarikçi ekranları
│   ├── utils/      # Yardımcı fonksiyonlar
│   └── main.dart
└── pubspec.yaml

🗄️ Veritabanı Tasarımı (PostgreSQL)

users
| id | name | email | password | role (MARKET / SUPPLIER / ADMIN) | created_at |

markets
| id | user_id (FK → users.id) | name | address | phone | created_at |

suppliers
| id | user_id (FK → users.id) | company_name | phone | created_at |

orders
| id | market_id (FK → markets.id) | status (PENDING / DELIVERED) | created_at |

order_items
| id | order_id (FK → orders.id) | product_name | quantity | unit | price |

deliveries
| id | order_id (FK → orders.id) | supplier_id (FK → suppliers.id) | delivery_status (IN_PROGRESS, DELIVERED) | delivery_time | route_info (JSON) |

🔗 İlişkiler:

Market → Order → OrderItem

Supplier → Delivery → Order

Her order tek bir delivery kaydına bağlanır

📌 API Endpoint Akışı
Auth

POST /api/auth/register → Kullanıcı oluştur

POST /api/auth/login → JWT token al

Orders

POST /api/orders → Market sipariş oluşturur

GET /api/orders/market/{marketId} → Market kendi siparişlerini görür

GET /api/orders/supplier/all → Tedarikçi tüm siparişleri görür

Deliveries

POST /api/deliveries/plan → Tedarikçi siparişler için rota planlar

PUT /api/deliveries/{deliveryId}/complete → Teslimatı tamamla

Reports

GET /api/reports/daily?supplierId=5&date=2025-09-09 → Günlük rapor JSON

GET /api/reports/daily/pdf?supplierId=5&date=2025-09-09 → Günlük rapor PDF

📊 Genel Süreç

Market sahibi → Sipariş oluşturur

Tedarikçi → Siparişleri listeler

Sistem → Rota planlar (Google Maps / OpenRouteService)

Tedarikçi → Siparişleri teslim eder

Sistem → Günlük/haftalık rapor + PDF üretir

🛠️ Geliştirme Adımları (MVP)

Kullanıcı kimlik doğrulama (JWT + Spring Security)

Sipariş oluşturma ve listeleme

Tedarikçi tarafında tüm siparişleri görme

Rota planlama entegrasyonu (Maps API)

Teslimat onay süreci

PDF raporlama