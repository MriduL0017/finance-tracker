package com.finance.tracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {
	
	@Value("${python.ai.url}") 
	private String pythonUrl;

    public String autoCategorize(String description) {    	
    	
        // 1. The URL of your Python microservice
        String pythonApiUrl = pythonUrl + "/categorize";
        
        // 2. Create the digital waiter
        RestTemplate restTemplate = new RestTemplate();
        
        // 3. Build the JSON request body: {"description": "your text here"}
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("description", description);
        
        try {
            // 4. Send the POST request to Python and wait for the Map (JSON dictionary) response
            Map<String, String> response = restTemplate.postForObject(pythonApiUrl, requestBody, Map.class);
            
            // 5. Extract the "category" value Python sent back
            return response != null ? response.getOrDefault("category", "Other") : "Other";
            
        } catch (Exception e) {
            // If the Python server is offline, just default to "Other" so the Java app doesn't crash!
            System.out.println("AI Service is down! Defaulting to 'Other'.");
            return "Other";
        }
    }
}