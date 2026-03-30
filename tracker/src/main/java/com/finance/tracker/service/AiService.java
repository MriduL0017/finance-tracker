package com.finance.tracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {
    
    @Value("${python.ai.url}") 
    private String pythonUrl;

    // --- HELPER: Bulletproof URL Builder ---
    private String getBaseUrl() {
        // If the environment variable accidentally ends with a "/", remove it!
        if (pythonUrl != null && pythonUrl.endsWith("/")) {
            return pythonUrl.substring(0, pythonUrl.length() - 1);
        }
        return pythonUrl;
    }

    // --- ENDPOINT 1: TEXT CATEGORIES ---
    public String autoCategorize(String description) {       
        String pythonApiUrl = getBaseUrl() + "/categorize";
        RestTemplate restTemplate = new RestTemplate();
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", description);
        
        try {
            Map<String, String> response = restTemplate.postForObject(pythonApiUrl, requestBody, Map.class);
            return response != null ? response.getOrDefault("category", "Other") : "Other";
        } catch (Exception e) {
            System.out.println("AI Service is down! Defaulting to 'Other'.");
            return "Other";
        }
    }

    // --- ENDPOINT 2: NATIVE VISION (RECEIPT SCANNER) ---
    public Map<String, Object> scanReceipt(MultipartFile file) {
        // 1. Build the foolproof URL
        String pythonApiUrl = getBaseUrl() + "/read-receipt";
        RestTemplate restTemplate = new RestTemplate();

        // 2. Set headers to tell Python an image file is coming
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 3. Package the file into a MultiValueMap
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        try {
            // Spring Boot requires us to wrap the file bytes in a Resource
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    // Provide a dummy filename so Python recognizes it as a file
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt.jpg";
                }
            };
            
            // The key "file" MUST match the parameter name in your Python FastAPI code!
            body.add("file", fileAsResource);

            // 4. Send the POST request
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 5. Wait for Python to send back the JSON Map
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(pythonApiUrl, requestEntity, Map.class);
            return response;

        } catch (Exception e) {
            System.out.println("AI Vision Service failed! " + e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "Vision AI offline or failed to parse.");
            fallback.put("category", "Other");
            return fallback;
        }
    }
}