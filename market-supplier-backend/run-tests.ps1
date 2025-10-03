# Run Individual Tests - Copy and Paste Commands

Write-Host "Available Test Commands (copy and paste):" -ForegroundColor Green
Write-Host "=" * 60

Write-Host "`n1. Basic Greeting Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Selam"})' -ForegroundColor White

Write-Host "`n2. View Cart Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Sepet"})' -ForegroundColor White

Write-Host "`n3. Clear Cart Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Sepetteki urunleri silmek istiyorum"})' -ForegroundColor White

Write-Host "`n4. Get Products Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Urunleri gormek istiyorum"})' -ForegroundColor White

Write-Host "`n5. Add Product Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="5 koli Falim sakiz"})' -ForegroundColor White

Write-Host "`n6. Pagination Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Baska var mi"})' -ForegroundColor White

Write-Host "`n7. Negative Intent Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Albeniyi sil istemiyorum"})' -ForegroundColor White

Write-Host "`n8. Chat Mode Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Nasilsin reis"})' -ForegroundColor White

Write-Host "`n9. Cart Status API:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/cart-status?phone=905414110375" -Method GET' -ForegroundColor White

Write-Host "`n10. Intent Analysis API:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/analyze-intent" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Sepette ne var"})' -ForegroundColor White

Write-Host "`n" + "=" * 60
Write-Host "Usage: Copy any command above and paste into PowerShell" -ForegroundColor Cyan
Write-Host "Expected: AI-generated, context-aware responses" -ForegroundColor Green

# Quick test function
function Quick-Test {
    Write-Host "`nRunning quick basic test..." -ForegroundColor Magenta
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (ConvertTo-Json @{phone="905414110375"; message="Selam"})
    Write-Host "Result: $($result.response)" -ForegroundColor Green
}

Write-Host "`nQuick test available: Run 'Quick-Test' command" -ForegroundColor Magenta
