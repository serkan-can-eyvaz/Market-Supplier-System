# One-Liner Test Commands - Copy & Paste Ready
# Bu komutları doğrudan PowerShell'e yapıştırabilirsiniz

Write-Host "🚀 One-Liner Test Commands (Copy & Paste)" -ForegroundColor Green
Write-Host "=" * 70

Write-Host "`n1️⃣ Basic Greeting Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Selam"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n2️⃣ View Cart Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Sepet"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n3️⃣ Clear Cart Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Sepetteki ürünleri silmek istiyorum"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n4️⃣ Get Products Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Ürünleri görmek istiyorum"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n5️⃣ Add Product Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="5 koli Falım sakız"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n6️⃣ Pagination Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Başka var mı"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n7️⃣ Encoding Issue Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="¦ptal"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n8️⃣ Negative Intent Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Albeniyi sil istemiyorum"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n9️⃣ Chat Mode Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Nasılsın reis"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n🔟 Cart Status API Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/cart-status?phone=905414110375" -Method GET -Headers @{"Content-Type"="application/json"}' -ForegroundColor White

Write-Host "`n1️⃣1️⃣ Intent Analysis API Test:" -ForegroundColor Yellow
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/analyze-intent" -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{phone="905414110375"; message="Sepette ne var"} | ConvertTo-Json)' -ForegroundColor White

Write-Host "`n" + "=" * 70
Write-Host "💡 Usage: Copy any command above and paste into PowerShell" -ForegroundColor Cyan
Write-Host "🎯 Expected: AI-generated, context-aware responses for all tests" -ForegroundColor Green
