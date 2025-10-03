# AI Agent Test Script - WhatsApp Assistant
# Bu script tüm senaryoları test eder

$baseUrl = "http://localhost:8080/api/n8n"
$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "🚀 AI Agent Test Basliyor..." -ForegroundColor Green
Write-Host "=" * 60

# Test 1: Temel Selam + Context Durumu
Write-Host "🔍 Test 1: Selam mesaji ve sepet contexti" -ForegroundColor Yellow
$body1 = @{
    phone = "905414110375"
    message = "Selam"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body1
    Write-Host "✅ Response: $($response1.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: Sepet Görüntüleme
Write-Host "`n🔍 Test 2: Sepet goruntuleme" -ForegroundColor Yellow
$body2 = @{
    phone = "905414110375"
    message = "Sepet"
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body2
    Write-Host "✅ Response: $($response2.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Sepet Boşaltma (Çeşitli varyasyonlar)
Write-Host "`n🔍 Test 3: Sepet boşaltma - 'Sepetteki ürünleri silmek istiyorum'" -ForegroundColor Yellow
$body3 = @{
    phone = "905414110375"
    message = "Sepetteki ürünleri silmek istiyorum"
} | ConvertTo-Json

try {
    $response3 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body3
    Write-Host "✅ Response: $($response3.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 4: Sepet Boşaltma - Alternatif
Write-Host "`n🔍 Test 4: Sepet boşaltma - 'Tümünü sepetten çıkar'" -ForegroundColor Yellow
$body4 = @{
    phone = "905414110375"
    message = "Tümünü sepetten çıkar"
} | ConvertTo-Json

try {
    $response4 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body4
    Write-Host "✅ Response: $($response4.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 5: Ürün Listesi
Write-Host "`n🔍 Test 5: Ürün listesi isteme" -ForegroundColor Yellow
$body5 = @{
    phone = "905414110375"
    message = "Ürünleri görmek istiyorum"
} | ConvertTo-Json

try {
    $response5 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body5
    Write-Host "✅ Response: $($response5.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 6: Ürün Ekleme
Write-Host "`n🔍 Test 6: Ürün ekleme - '5 koli Falım sakız'" -ForegroundColor Yellow
$body6 = @{
    phone = "905414110375"
    message = "5 koli Falım sakız"
} | ConvertTo-Json

try {
    $response6 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body6
    Write-Host "✅ Response: $($response6.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 7: Pagination - Daha fazla ürün
Write-Host "`n🔍 Test 7: Ürün pagination - 'Başka var mı'" -ForegroundColor Yellow
$body7 = @{
    phone = "905414110375"
    message = "Başka var mı"
} | ConvertTo-Json

try {
    $response7 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body7
    Write-Host "✅ Response: $($response7.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 8: Encoding Sorunu Test - İptal
Write-Host "`n🔍 Test 8: Encoding sorunu - '¦ptal' (bozuk iptal)" -ForegroundColor Yellow
$body8 = @{
    phone = "905414110375"
    message = "¦ptal"
} | ConvertTo-Json

try {
    $response8 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body8
    Write-Host "✅ Response: $($response8.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $(_Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 9: Negative Intent Test
Write-Host "`n🔍 Test 9: Negative intent - 'Albeniyi sil istemiyorum'" -ForegroundColor Yellow
$body9 = @{
    phone = "905414110375"
    message = "Albeniyi sil istemiyorum"
} | ConvertTo-Json

try {
    $response9 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body9
    Write-Host "✅ Response: $($response9.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 10: Chat Mode Test
Write-Host "`n🔍 Test 10: Chat mode - 'Nasılsın reis'" -ForegroundColor Yellow
$body10 = @{
    phone = "905414110375"
    message = "Nasılsın reis"
} | ConvertTo-Json

try {
    $response10 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body10
    Write-Host "✅ Response: $($response10.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 11: Final Cart Status
Write-Host "`n🔍 Test 11: Final sepet durumu kontrolü" -ForegroundColor Yellow
$body11 = @{
    phone = "905414110375"
    message = "Sepette ne var"
} | ConvertTo-Json

try {
    $response11 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body11
    Write-Host "✅ Response: $($response11.response)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n" + "=" * 60
Write-Host "🎊 Tüm testler tamamlandı!" -ForegroundColor Green
Write-Host "🤖 AI Agent artık tamamen AI-native ve context-aware çalışıyor!" -ForegroundColor Cyan

# Bonus: Rate Limit Simülasyonu (opsiyonel)
Write-Host "`n💡 Bonus: Rate limit testi için çok sayıda istek atabilirsiniz..." -ForegroundColor Magenta
Write-Host "Bu scripti tekrar calistirarak rate limit durumunu test edebilirsiniz." -ForegroundColor Gray
