# AI Tabanlı WhatsApp Asistanı

Bu sistem artık tamamen **AI tabanlı, context-aware ve state-driven** bir WhatsApp asistanıdır. Kelime bazlı contains/if-else mantığı kaldırılmış, her yanıt AI tarafından context ve state'e göre dinamik olarak üretilmektedir.

## 🎯 Ana Özellikler

### ✅ Tamamen AI Tabanlı Sistem
- **Hiç sabit yanıt yok** - her cevap AI tarafından context'e göre üretilir
- **Kelime bazlı kontroller kaldırıldı** - tamamen doğal dil anlayışı
- **Edge case'ler AI tarafından yönetilir** - sabit şablonlar yok

### ✅ Mod Seçimi 
- **1: Yapay zeka ile sipariş oluşturma** (default)
- **2: Sohbet modu** 
- Sadece kullanıcı "mod" dediğinde gösterilir
- AI tarafından context'e göre yönetilir

### ✅ Sepet Yönetimi
- **AI tabanlı state yönetimi** - güncel sepet durumuna göre yanıt
- **Bulanık eşleşme** - typo'lar ve varyasyonlar anlayışı
- **Contextual yanıtlar** - "sepeti boşalt", "her şeyi sil" gibi varyasyonlar
- **Tek ürün otomatik çıkarma** - context'e göre akıllı karar

### ✅ Context & State Aware
- **Sepet durumu** her yanıtta dikkate alınır
- **Konuşma geçmişi** context olarak kullanılır
- **Session data** ile mod ve state yönetimi
- **Gerçek zamanlı cart güncellemeleri**

## 🏗️ Sistem Mimarisi

### Core Components (Güncellendi)

1. **LLMIntentExtractor**: Tamamen AI tabanlı intent çıkarımı
2. **AgentOrchestrator**: Context-aware orchestration 
3. **AIAgentService**: State-driven yanıt üretimi
4. **N8nWorkflowController**: n8n entegrasyonu için endpoint'ler

### AI Response Generation

```
User Message → Intent Classification (AI) → Context Update → 
Action Execution → AI Response Generation → State Update
```

Her adımda AI context ve state bilgisini kullanır.

## 🔗 n8n Workflow Alternatifi

### Workflow Yapısı:
```
WhatsApp Trigger → AI Node → Switch/IF Node → 
HTTP Request Node → WhatsApp Response Node
```

### Endpoint'ler:
- `POST /api/n8n/whatsapp/message` - Ana mesaj işleme
- `POST /api/n8n/intent/analyze` - Intent analizi  
- `GET /api/n8n/cart/status/{phone}` - Sepet durumu
- `POST /api/n8n/command/execute` - Özel komutlar
- `GET /api/n8n/health` - Health check

## 📱 Kullanım Örnekleri

### Senaryo 1: Selamlama + Sepet Durumu
```
Kullanıcı: "Merhaba"
AI: "Merhaba! Sepetinizde 5 adet Albeni paketi var ve toplam tutar 1324,95 TL. 
     Sipariş oluşturmak veya sohbet etmek ister misiniz?"
```

### Senaryo 2: Sepet Temizleme  
```
Kullanıcı: "Sepeti boşalt"
AI: "✅ Sepetiniz başarılı bir şekilde boşaltıldı. Şimdi sepette hiçbir ürün 
     bulunmuyor ve toplam tutar 0,00 TL'dir."
```

### Senaryo 3: Ürün Çıkarma
```
Kullanıcı: "Albeni çıkar" 
AI: "Sepetinizde artık Albeni bulunmamaktadır."
```

### Senaryo 4: Boş Sepet Kontrolü
```
Kullanıcı: "Sepette ne var"
AI: "Sepetinizde şu anda ürün bulunmamaktadır."
```

## 🛠️ Konfigürasyon

### AI Settings
```properties
# LLM API Configuration
GROQ_API_KEY=your_api_key
AI_API_BASE_URL=https://api.groq.com/openai/v1
AI_MODEL=llama3-8b-8192

# AI Agent Settings  
ai.agent.enabled=true
ai.timeout=30000
ai.confidence.threshold=0.7
```

### Mode Management
- Default: `ordering` (sipariş modu)
- Session'da `mode` key ile saklanır
- AI tarafından context'e göre yönetilir

## 🎪 Edge Cases (AI Tarafından Yönetilen)

### ✅ Otomatik Yönetilen Durumlar:
- Sepetteki ürün sayısı kontrolü
- İsim eşleşmeleri ve typo'lar  
- Hatalı girişler ve eksik bilgiler
- Boş sepet durumları
- Duplicate onaylar
- Aşırı miktarlar
- Stok kontrolleri
- Birim karışıklıkları
- Kategori istekleri
- Yanlış yazımlar

### AI Örnekleri:
```
"sepeti boşalt" ≈ "her şeyi sil" ≈ "hepsini sil" → clear_cart
"kola" + typo → AI fuzzy matching
"5000 koli cola" → AI excessive amount warning  
"meyve lazım" → AI contextual question asking
```

## 🚀 Dağıtım Seçenekleri

### 1. Backend Entegrasyonu
Mevcut Spring Boot uygulamanızla doğrudan entegre

### 2. n8n Workflow  
```json
{
  "nodes": [
    {"type": "WhatsApp Trigger"},
    {"type": "HTTP Request", "url": "/api/n8n/whatsapp/message"},
    {"type": "Switch", "based_on": "intent_category"},
    {"type": "WhatsApp Response"}
  ]
}
```

## 📊 Monitoring & Analytics

- **Intent analytics** - hangi intent'ler kullanılıyor
- **Context tracking** - sepet durumu değişimleri
- **AI performance** - yanıt süreleri ve kalitesi
- **Edge case handling** - AI'nın çözdüğü özel durumlar

## 🔧 Development Notes

### Yeni Özellik Ekleme:
1. Intent'i `LLMIntentExtractor`'a ekle
2. Context handling logic'i `AgentOrchestrator`'a ekle  
3. AI prompt'larını güncelle
4. Test case'leri context ile birlikte yaz

### AI Prompt Engineering:
- System prompt'lar context-aware
- Few-shot örnekler Türkçe
- State-based instructions
- Edge case handling guidelines

## 🎭 AI Personality

**MarketAsist** olarak:
- Profesyonel ama samimi
- Context'e duyarlı
- State-aware yanıtlar
- Türkçe doğal dil
- Hiç template kullanmaz
- Edge case'leri akıllıca çözer

---

**🎉 Artık sistemin tamamı AI tabanlı, context-aware ve edge case'leri kapalı olarak çalışmaktadır!**

Bot mantığı yok, kelime bazlı kontroller yok, sabit yanıtlar yok - sadece akıllı AI yanıtları! 🤖✨
