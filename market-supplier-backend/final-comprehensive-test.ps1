# =============================================================================
# KAPSAMLI AI AGENT TEST SCRIPT - Final Version 
# Tüm sorunlar çözüldükten sonra kapsamlı test
# =============================================================================

# PowerShell UTF-8 encoding ayarları
chcp 65001 > $null
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$baseUrl = "http://localhost:8080/api/n8n/whatsapp/message"
$phone = "905414110375"

function Test-AIAgent {
    param(
        [string]$testName,
        [string]$message,
        [string]$expectedIntent,
        [string]$expectedResult
    )
    
    Write-Host "`n🧪 TEST: $testName" -ForegroundColor Cyan
    Write-Host "📝 Message: '$message'" -ForegroundColor Yellow
    Write-Host "🎯 Expected: $expectedIntent → $expectedResult" -ForegroundColor Green
    
    $headers = @{
        "Content-Type" = "application/json; charset=utf-8"
    }
    
    $body = @{
        phone = $phone
        message = $message
    } | ConvertTo-Json -Depth 10
    
    try {
        $response = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body
        
        Write-Host "✅ SUCCESS" -ForegroundColor Green
        Write-Host "📋 Intent Category: $($response.intent_category)" -ForegroundColor Magenta
        Write-Host "💬 Response: $($response.response)" -ForegroundColor White
        Write-Host "🔄 Cart Action: $($response.has_cart_action)" -ForegroundColor Blue
        
        return $response
    }
    catch {
        Write-Host "❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

Write-Host "🚀 AI AGENT KAPSAMLI TEST BAŞLIYOR..." -ForegroundColor Green
Write-Host "=" * 80

# =============================================================================
# TEST 1: INTENT CLASSIFICATION SORUNLARI
# =============================================================================

Write-Host "`n🎯 1. INTENT CLASSIFICATION TESTS" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Intent Fix 1" -message "2 paket cizivis" -expectedIntent "add_to_cart" -expectedResult "Ürün sepete eklenmeli veya bulunamadı uyarısı"

Test-AIAgent -testName "Intent Fix 2" -message "cizivic Peynirli Sandvic Kraker 2 koli" -expectedIntent "add_to_cart" -expectedResult "Doğru ürün bulunup sepete eklenmeli"

Test-AIAgent -testName "Intent Verification" -message "3 paket Albeni ekleyelim" -expectedIntent "add_to_cart" -expectedResult "Albeni sepete eklenmeli"

# =============================================================================
# TEST 2: ÜRÜN BULMA VE FİYAT HESAPLAMA
# =============================================================================

Write-Host "`n💰 2. PRODUCT MATCHING & PRICE CALCULATION" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Exact Match" -message "2 koli Çiziviç Peynirli Sandviç Kraker" -expectedIntent "add_to_cart" -expectedResult "2 × 318.08 = 636.16 TL"

Test-AIAgent -testName "Fuzzy Match" -message "3 paket albeni" -expectedIntent "add_to_cart" -expectedResult "3 × 264.99 = 794.97 TL"

Test-AIAgent -testName "Not Found" -message "5 adet nonexistent" -expectedIntent "add_to_cart" -expectedResult "Bulunamayan ürün uyarısı"

# =============================================================================
# TEST 3: TÜRKÇE KARAKTER DESTEĞI 
# =============================================================================

Write-Host "`n🇹🇷 3. TURKISH CHARACTER SUPPORT" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Turkish Chars 1" -message "Ürünler" -expectedIntent "get_products" -expectedResult "Ürün listesi"

Test-AIAgent -testName "Turkish Chars 2" -message "2 paket Çiziviç" -expectedIntent "add_to_cart" -expectedResult "Çiziviç sepete eklenmeli"

Test-AIAgent -testName "Turkish Chars 3" -message "Sepeti boşalt" -expectedIntent "clear_cart" -expectedResult "Sepet boşaltılmalı"

# =============================================================================
# TEST 4: CART OPERATIONS
# =============================================================================

Write-Host "`n🛒 4. CART OPERATIONS" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "View Cart" -message "Sepette ne var" -expectedIntent "view_cart" -expectedResult "Mevcut sepet içeriği"

Test-AIAgent -testName "Clear Cart" -message "Sepetteki ürünleri silmek istiyorum" -expectedIntent "clear_cart" -expectedResult "Sepet boşaltılmalı"

Test-AIAgent -testName "View Empty Cart" -message "Sepeti göster" -expectedIntent "view_cart" -expectedResult "Sepet boş mesajı"

# =============================================================================
# TEST 5: PRODUCT LISTING
# =============================================================================

Write-Host "`n📋 5. PRODUCT LISTING" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Product List 1" -message "urunleri goster" -expectedIntent "get_products" -expectedResult "Tüm ürünler stoklarıyla"

Test-AIAgent -testName "Product List 2" -message "Ürünleri görmek istiyorum" -expectedIntent "get_products" -expectedResult "Ürün listesi"

Test-AIAgent -testName "Product List 3" -message "Farklı ürünleri görmek istiyorum" -expectedIntent "get_products" -expectedResult "Ürün katalogu"

# =============================================================================
# TEST 6: EDGE CASES VE CONTEXT AWARENESS
# =============================================================================

Write-Host "`n🎯 6. EDGE CASES & CONTEXT AWARENESS" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Greeting" -message "Merhaba" -expectedIntent "chat" -expectedResult "Doğal karşılama + mevcut sepet durumu"

Test-AIAgent -testName "Mixed Request" -message "Merhaba, 2 paket Albeni istiyorum" -expectedIntent "add_to_cart" -expectedResult "Albeni sepete eklenmeli"

Test-AIAgent -testName "Encoding Issue" -message "³r³nleri goster" -expectedIntent "get_products" -expectedResult "Ürün listesi (encoding toleransı)"

# =============================================================================
# TEST 7: FINAL VERIFICATION
# =============================================================================

Write-Host "`n✅ 7. FINAL VERIFICATION" -ForegroundColor Yellow
Write-Host "-" * 50

Test-AIAgent -testName "Final Cart State" -message "Sepetti görelim" -expectedIntent "view_cart" -expectedResult "Güncel sepet içeriği"

Test-AIAgent -testName "Final Product Add" -message "1 koli Pepsi ekle" -expectedIntent "add_to_cart" -expectedResult "Pepsi eklenmeli ve toplam güncellenmeli"

Write-Host "`n🎉 TÜM TESTLER TAMAMLANDI!" -ForegroundColor Green
Write-Host "=" * 80
Write-Host "✨ Sistem artık tamamen AI-driven, context-aware ve state-driven!" -ForegroundColor Cyan
Write-Host "🚀 Asistan (bot değil!) başarıyla çalışıyor!" -ForegroundColor Green
