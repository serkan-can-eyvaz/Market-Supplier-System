package com.example.marketsupplier.service;

import com.example.marketsupplier.dto.CartResponse;
import com.example.marketsupplier.dto.CartItemResponse;
import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderPdfService {

    public byte[] generateCartPdf(CartResponse cart) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Basit HTML tabanlı PDF oluşturma (iText kullanmadan)
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Sipariş Detayı</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".info { margin-bottom: 20px; }");
        html.append(".items { margin-bottom: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".total { font-weight: bold; font-size: 18px; }");
        html.append(".footer { margin-top: 30px; text-align: center; font-size: 12px; color: #666; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>Market Sipariş Detayı</h1>");
        html.append("<p>Oluşturma Tarihi: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        
        // Market Bilgileri
        html.append("<div class='info'>");
        html.append("<h3>Market Bilgileri</h3>");
        html.append("<p><strong>Market Adı:</strong> ").append(cart.getMarketName()).append("</p>");
        html.append("<p><strong>Sepet ID:</strong> ").append(cart.getId()).append("</p>");
        html.append("</div>");
        
        // Ürünler
        html.append("<div class='items'>");
        html.append("<h3>Sipariş Edilen Ürünler</h3>");
        html.append("<table>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Ürün Adı</th>");
        html.append("<th>Birim</th>");
        html.append("<th>Miktar</th>");
        html.append("<th>Birim Fiyat</th>");
        html.append("<th>Toplam</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        for (CartItemResponse item : cart.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getProductName()).append("</td>");
            html.append("<td>").append(item.getProductUnit()).append("</td>");
            html.append("<td>").append(item.getQuantity()).append("</td>");
            html.append("<td>").append(String.format("%.2f TL", item.getProductPrice())).append("</td>");
            html.append("<td>").append(String.format("%.2f TL", item.getTotalPrice())).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        
        // Toplam
        html.append("<div class='total'>");
        html.append("<p>Toplam Ürün Sayısı: ").append(cart.getTotalItems()).append("</p>");
        html.append("<p>Genel Toplam: ").append(String.format("%.2f TL", cart.getTotalAmount())).append("</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Bu belge otomatik olarak oluşturulmuştur.</p>");
        html.append("<p>Market Tedarikçi Sistemi - ").append(LocalDateTime.now().getYear()).append("</p>");
        html.append("</div>");
        
        html.append("</body>");
        html.append("</html>");
        
        // HTML'i byte array'e dönüştür
        outputStream.write(html.toString().getBytes("UTF-8"));
        
        return outputStream.toByteArray();
    }
    
    public byte[] generateOrderPdf(Order order) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Sipariş Faturası</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".info { margin-bottom: 20px; }");
        html.append(".items { margin-bottom: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".total { font-weight: bold; font-size: 18px; }");
        html.append(".status { padding: 5px 10px; border-radius: 5px; }");
        html.append(".pending { background-color: #fff3cd; color: #856404; }");
        html.append(".approved { background-color: #d4edda; color: #155724; }");
        html.append(".delivered { background-color: #cce5ff; color: #004085; }");
        html.append(".footer { margin-top: 30px; text-align: center; font-size: 12px; color: #666; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>Sipariş Faturası</h1>");
        html.append("<p>Oluşturma Tarihi: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("</p>");
        html.append("</div>");
        
        // Sipariş Bilgileri
        html.append("<div class='info'>");
        html.append("<h3>Sipariş Bilgileri</h3>");
        html.append("<p><strong>Sipariş No:</strong> #").append(order.getId()).append("</p>");
        html.append("<p><strong>Market:</strong> ").append(order.getMarket().getName()).append("</p>");
        html.append("<p><strong>Durum:</strong> <span class='status ").append(order.getStatus().name().toLowerCase()).append("'>").append(getStatusText(order.getStatus().name())).append("</span></p>");
        html.append("</div>");
        
        // Ürünler
        html.append("<div class='items'>");
        html.append("<h3>Sipariş Edilen Ürünler</h3>");
        html.append("<table>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Ürün Adı</th>");
        html.append("<th>Birim</th>");
        html.append("<th>Miktar</th>");
        html.append("<th>Birim Fiyat</th>");
        html.append("<th>Toplam</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        for (OrderItem item : order.getItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getProductName()).append("</td>");
            html.append("<td>").append(item.getUnit()).append("</td>");
            html.append("<td>").append(item.getQuantity()).append("</td>");
            html.append("<td>").append(String.format("%.2f TL", item.getPrice())).append("</td>");
            html.append("<td>").append(String.format("%.2f TL", item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        html.append("</div>");
        
        // Toplam
        html.append("<div class='total'>");
        html.append("<p>Genel Toplam: ").append(String.format("%.2f TL", order.getTotalPrice())).append("</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Bu belge otomatik olarak oluşturulmuştur.</p>");
        html.append("<p>Market Tedarikçi Sistemi - ").append(LocalDateTime.now().getYear()).append("</p>");
        html.append("</div>");
        
        html.append("</body>");
        html.append("</html>");
        
        outputStream.write(html.toString().getBytes("UTF-8"));
        
        return outputStream.toByteArray();
    }
    
    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING": return "Beklemede";
            case "APPROVED": return "Onaylandı";
            case "DELIVERED": return "Teslim Edildi";
            case "CANCELLED": return "İptal Edildi";
            default: return status;
        }
    }
}