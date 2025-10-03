# Quick Test Functions - AI Agent
# Her testi ayrı ayrı çalıştırmak için

$baseUrl = "http://localhost:8080/api/n8n"
$headers = @{
    "Content-Type" = "application/json"
}

function Test-BasicGreeting {
    Write-Host "🔍 Testing: Basic Greeting" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Selam"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-ViewCart {
    Write-Host "🔍 Testing: View Cart" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Sepet"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-ClearCart {
    Write-Host "🔍 Testing: Clear Cart" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Sepetteki ürünleri silmek istiyorum"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-GetProducts {
    Write-Host "🔍 Testing: Get Products" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Ürünleri görmek istiyorum"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-AddProduct {
    Write-Host "🔍 Testing: Add Product" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "5 koli Falım sakız"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-Pagination {
    Write-Host "🔍 Testing: Product Pagination" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Başka var mı"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-EncodingIssue {
    Write-Host "🔍 Testing: Encoding Issue (¦ptal)" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "¦ptal"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-NegativeIntent {
    Write-Host "🔍 Testing: Negative Intent" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Albeniyi sil istemiyorum"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-ChatMode {
    Write-Host "🔍 Testing: Chat Mode" -ForegroundColor Yellow
    $body = @{
        phone = "905414110375"
        message = "Nasılsın reis"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/process-message" -Method POST -Headers $headers -Body $body
    Write-Host "Response: $($response.response)" -ForegroundColor Green
}

function Test-ContextStatus {
    Write-Host "🔍 Testing: Context Status" -ForegroundColor Yellow
    
    # Get cart status
    $cartResponse = Invoke-RestMethod -Uri "$baseUrl/cart-status?phone=905414110375" -Method GET -Headers $headers
    Write-Host "Cart Status: $($cartResponse | ConvertTo-Json)" -ForegroundColor Cyan
    
    # Test intent analysis
    $body = @{
        phone = "905414110375"
        message = "Sepette ne var"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/analyze-intent" -Method POST -Headers $headers -Body $body
    Write-Host "Intent Analysis: $($response | ConvertTo-Json)" -ForegroundColor Cyan
}

# Usage Instructions
Write-Host "🚀 Quick Test Functions Loaded!" -ForegroundColor Green
Write-Host "Kullanım:" -ForegroundColor Yellow
Write-Host "Test-BasicGreeting     # Temel selam testi"
Write-Host "Test-ViewCart          # Sepet görüntüleme"
Write-Host "Test-ClearCart         # Sepet temizleme"
Write-Host "Test-GetProducts       # Ürün listesi"
Write-Host "Test-AddProduct        # Ürün ekleme"
Write-Host "Test-Pagination        # Sayfalama"
Write-Host "Test-EncodingIssue     # Encoding sorunu"
Write-Host "Test-NegativeIntent    # Negatif intent"
Write-Host "Test-ChatMode          # Chat modu"
Write-Host "Test-ContextStatus     # Context durumu"
Write-Host ""
Write-Host "Örnek: Test-BasicGreeting" -ForegroundColor Cyan
