package com.example.marketsupplier.service;

import com.example.marketsupplier.entity.Order;
import com.example.marketsupplier.entity.OrderItem;
import com.example.marketsupplier.repository.OrderItemRepository;
import com.example.marketsupplier.repository.OrderRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderPdfService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public byte[] generateOrderPdf(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        doc.add(new Paragraph("Sipariş #" + order.getId()).setBold());
        doc.add(new Paragraph("Market: " + order.getMarket().getName()));
        doc.add(new Paragraph("Adres: " + order.getMarket().getAddress()));
        doc.add(new Paragraph("Durum: " + order.getStatus()));
        doc.add(new Paragraph("Tarih: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 15, 15, 15, 15})).useAllAvailableWidth();
        table.addHeaderCell("Ürün");
        table.addHeaderCell("Adet");
        table.addHeaderCell("Birim");
        table.addHeaderCell("Fiyat");
        table.addHeaderCell("Tutar");

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            BigDecimal line = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            total = total.add(line);
            table.addCell(i.getProductName());
            table.addCell(String.valueOf(i.getQuantity()));
            table.addCell(i.getUnit());
            table.addCell("" + i.getPrice());
            table.addCell("" + line);
        }

        doc.add(table);
        doc.add(new Paragraph("Toplam: " + total));

        doc.close();
        return baos.toByteArray();
    }

    public byte[] generateOrderHistoryPdf(List<Order> orders) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        doc.add(new Paragraph("Geçmiş Sipariş Dökümü").setBold().setFontSize(20));
        doc.add(new Paragraph("Oluşturulma Tarihi: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        for (Order order : orders) {
            doc.add(new Paragraph("\n-----------------------------------\n").setBold());
            doc.add(new Paragraph("Sipariş #" + order.getId()).setBold());
            doc.add(new Paragraph("Durum: " + order.getStatus()));
            doc.add(new Paragraph("Tarih: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 15, 15, 15, 15})).useAllAvailableWidth();
            table.addHeaderCell("Ürün");
            table.addHeaderCell("Adet");
            table.addHeaderCell("Birim");
            table.addHeaderCell("Fiyat");
            table.addHeaderCell("Tutar");

            BigDecimal orderTotal = BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                orderTotal = orderTotal.add(lineTotal);
                table.addCell(item.getProductName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(item.getUnit());
                table.addCell(item.getPrice().toString());
                table.addCell(lineTotal.toString());
            }
            doc.add(table);
            doc.add(new Paragraph("Sipariş Toplamı: " + orderTotal.toString()).setBold());
        }

        doc.close();
        return baos.toByteArray();
    }
}


