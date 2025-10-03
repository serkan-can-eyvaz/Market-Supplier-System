package com.example.marketsupplier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;


@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${app.whatsapp.access_token:}")
    private String accessToken;

    @Value("${app.whatsapp.phone_number_id:}")
    private String phoneNumberId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Autowired
    private CriticalServiceWrapper criticalServiceWrapper;

    public WhatsAppService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public void sendTextMessage(String toPhoneE164, String messageBody) {
        criticalServiceWrapper.executeWhatsAppOperationWithRetry(toPhoneE164, messageBody, () -> {
            sendTextMessageInternal(toPhoneE164, messageBody);
        });
    }

    private void sendTextMessageInternal(String toPhoneE164, String messageBody) {
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("WhatsApp credentials missing. Skipping send. phoneNumberIdPresent={} accessTokenPresent={}",
                (phoneNumberId != null && !phoneNumberId.isBlank()), (accessToken != null && !accessToken.isBlank()));
            return;
        }

        String url = String.format("https://graph.facebook.com/v19.0/%s/messages", phoneNumberId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toPhoneE164);
        Map<String, Object> text = new HashMap<>();
        text.put("body", messageBody);
        payload.put("text", text);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String response = restTemplate.postForObject(url, entity, String.class);
            log.info("WA send response: {}", response);
        } catch (Exception ex) {
            log.warn("Failed to send WhatsApp message to {}", toPhoneE164, ex);
        }
    }

    public void sendDocument(String toPhoneE164, byte[] fileContent, String filename, String caption) {
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("WhatsApp credentials missing. Skipping send document.");
            return;
        }

        try {
            // Step 1: Upload the document to get a media ID
            String mediaId = uploadDocument(fileContent, filename);
            if (mediaId == null) {
                log.error("Failed to upload document to WhatsApp, media ID is null.");
                sendTextMessage(toPhoneE164, "PDF belgesi gönderilirken bir hata oluştu.");
                return;
            }

            // Step 2: Send the document message using the media ID
            String url = String.format("https://graph.facebook.com/v19.0/%s/messages", phoneNumberId);
            
            Map<String, Object> document = new HashMap<>();
            document.put("id", mediaId);
            document.put("filename", filename);
            document.put("caption", caption);

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", toPhoneE164);
            payload.put("type", "document");
            payload.put("document", document);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Successfully sent document message. Response: {}", response.body());
            } else {
                log.error("Failed to send document message. Status: {}, Response: {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Exception occurred while sending document to {}", toPhoneE164, e);
        }
    }

    private String uploadDocument(byte[] fileContent, String filename) {
        try {
            String url = String.format("https://graph.facebook.com/v19.0/%s/media", phoneNumberId);
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();

            Map<Object, Object> data = new HashMap<>();
            data.put("file", fileContent);
            data.put("filename", filename);
            data.put("type", "application/pdf");
            data.put("messaging_product", "whatsapp");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(ofMimeMultipartData(data, boundary))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseNode = objectMapper.readTree(response.body());
                String mediaId = responseNode.get("id").asText();
                log.info("Successfully uploaded media, ID: {}", mediaId);
                return mediaId;
            } else {
                log.error("Failed to upload media. Status: {}, Response: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception during media upload", e);
            return null;
        }
    }

    private HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data, String boundary) throws java.io.IOException {
        var byteArrays = new java.util.ArrayList<byte[]>();
        String separator = "--" + boundary + "\r\nContent-Disposition: form-data; name=";

        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator.getBytes());
            if (entry.getValue() instanceof byte[]) {
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + data.get("filename") + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes());
                byteArrays.add((byte[]) entry.getValue());
                byteArrays.add("\r\n".getBytes());
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes());
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes());
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}


