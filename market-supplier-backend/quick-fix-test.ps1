# =============================================================================
# HIZLI DÜZELTME TESTLERİ - Sadece Sorunlu Alanlar
# =============================================================================

chcp 65001 > $null
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$baseUrl = "http://localhost:8080/api/n8n/whatsapp/message"
$phone = "905414110375"

function Quick-Test {
    param([string]$name, [string]$message)
    
    Write-Host "`n🧪 $name" -ForegroundColor Cyan
    Write-Host "📝 '$message'" -ForegroundColor Yellow
    
    $body = @{phone = $phone; message = $message} | ConvertTo-Json
    $headers = @{"Content-Type" = "application/json; charset=utf-8"}
    
    try {
        $r = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body
        Write-Host "✅ Intent: $($r.intent_category) | Cart: $($r.has_cart_action)" -ForegroundColor Green
        Write-Host "💬 Response: $($r.response.Substring(0, [Math]::Min(100, $r.response.Length)))..." -ForegroundColor White
    }
    catch {
        Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "🔧 HIZLI DÜZELTME TESTLERİ" -ForegroundColor Green

# Esas sorunlu testler
Quick-Test "INTENT-1" "2 paket cizivis"
Quick-Test "INTENT-2" "cizivic Peynirli Sandvic Kraker 2 koli" 
Quick-Test "TÜRKÇE" "Çiziviç Peynirli Sandviç Kraker 2 koli"
Quick-Test "ÜRÜN-LISTESI" "urunleri goster"
Quick-Test "SEPET-KONTROL" "Sepette ne var"

Write-Host "`n🎯 Test tamamlandı!" -ForegroundColor Green
