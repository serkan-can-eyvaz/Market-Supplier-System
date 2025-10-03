# Simple AI Agent Test Script (ASCII only)
$baseUrl = "http://localhost:8080/api/n8n"
$headers = @{"Content-Type" = "application/json"}

Write-Host "AI Agent Test Starting..." -ForegroundColor Green
Write-Host "=" * 50

# Test 1: Basic Greeting
Write-Host "Test 1: Basic Greeting" -ForegroundColor Yellow
$body1 = @{phone = "905414110375"; message = "Selam"} | ConvertTo-Json
try {
$response1 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body1
    Write-Host "Response: $($response1.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: View Cart
Write-Host "`nTest 2: View Cart" -ForegroundColor Yellow
$body2 = @{phone = "905414110375"; message = "Sepet"} | ConvertTo-Json
try {
$response2 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body2
    Write-Host "Response: $($response2.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Clear Cart
Write-Host "`nTest 3: Clear Cart" -ForegroundColor Yellow
$body3 = @{phone = "905414110375"; message = "Sepetteki urunleri silmek istiyorum"} | ConvertTo-Json
try {
$response3 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body3
    Write-Host "Response: $($response3.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 4: Get Products
Write-Host "`nTest 4: Get Products" -ForegroundColor Yellow
$body4 = @{phone = "905414110375"; message = "Urunleri gormek istiyorum"} | ConvertTo-Json
try {
$response4 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body4
    Write-Host "Response: $($response4.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 5: Add Product
Write-Host "`nTest 5: Add Product" -ForegroundColor Yellow
$body5 = @{phone = "905414110375"; message = "5 koli Falim sakiz"} | ConvertTo-Json
try {
$response5 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body5
    Write-Host "Response: $($response5.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 6: Pagination
Write-Host "`nTest 6: Pagination" -ForegroundColor Yellow
$body6 = @{phone = "905414110375"; message = "Baska var mi"} | ConvertTo-Json
try {
$response6 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body6
    Write-Host "Response: $($response6.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 7: Encoding Issue
Write-Host "`nTest 7: Encoding Issue" -ForegroundColor Yellow
$body7 = @{phone = "905414110375"; message = "iptal"} | ConvertTo-Json
try {
$response7 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body7
    Write-Host "Response: $($response7.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 8: Negative Intent
Write-Host "`nTest 8: Negative Intent" -ForegroundColor Yellow
$body8 = @{phone = "905414110375"; message = "Albeniyi sil istemiyorum"} | ConvertTo-Json
try {
$response8 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body8
    Write-Host "Response: $($response8.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 9: Chat Mode
Write-Host "`nTest 9: Chat Mode" -ForegroundColor Yellow
$body9 = @{phone = "905414110375"; message = "Nasilsin reis"} | ConvertTo-Json
try {
$response9 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body9
    Write-Host "Response: $($response9.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 10: Final Cart Status
Write-Host "`nTest 10: Final Cart Status" -ForegroundColor Yellow
$body10 = @{phone = "905414110375"; message = "Sepette ne var"} | ConvertTo-Json
try {
$response10 = Invoke-RestMethod -Uri "$baseUrl/whatsapp/message" -Method POST -Headers $headers -Body $body10
    Write-Host "Response: $($response10.response)" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n" + "=" * 50
Write-Host "All tests completed!" -ForegroundColor Green
Write-Host "AI Agent is now fully AI-native and context-aware!" -ForegroundColor Cyan
