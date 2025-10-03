# =============================================================================
# TEK TEK KOMUT TESTLERİ - Copy-Paste İçin
# =============================================================================

# PowerShell UTF-8 
chcp 65001 > $null; [Console]::InputEncoding = [System.Text.Encoding]::UTF8; [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# TEST 1: Intent sınıflandırma sorunu (en önemli)
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="2 paket cizivis"} | ConvertTo-Json)

# TEST 2: İkinci intent sorunu  
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="cizivic Peynirli Sandvic Kraker 2 koli"} | ConvertTo-Json)

# TEST 3: Türkçe karakter sorunu
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="Çiziviç Peynirli Sandviç Kraker 2 koli"} | ConvertTo-Json)

# TEST 4: Ürün listesi kontrolü
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="urunleri goster"} | ConvertTo-Json)

# TEST 5: Sepet kontrol
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="Sepette ne var"} | ConvertTo-Json)

# TEST 6: Doğru intent testi
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="3 paket Albeni ekleyelim"} | ConvertTo-Json)

# TEST 7: Sepet temizleme 
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="Sepeti boşalt"} | ConvertTo-Json)

# TEST 8: Fuzzy matching
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="2 paket albeni"} | ConvertTo-Json)

# TEST 9: Encoding toleransı
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="³r³nleri goster"} | ConvertTo-Json)

# TEST 10: Final verification
Invoke-RestMethod -Uri "http://localhost:8080/api/n8n/whatsapp/message" -Method POST -Headers @{"Content-Type"="application/json; charset=utf-8"} -Body (@{phone="905414110375"; message="Merhaba"} | ConvertTo-Json)
