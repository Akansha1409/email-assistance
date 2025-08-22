package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        // Build the prompt
        String prompt = buildPrompt(emailRequest);

        // Craft a request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        // **KEY DEBUGGING STEP 1:** Log the full URL being used.
        String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;
        System.out.println("Full API URL: " + fullUrl);

        // **KEY DEBUGGING STEP 2:** Log the raw JSON string of the request body.
        try {
            String jsonRequestBody = new ObjectMapper().writeValueAsString(requestBody);
            System.out.println("JSON Request Body: " + jsonRequestBody);
        } catch (Exception e) {
            System.err.println("Error converting request body to JSON: " + e.getMessage());
        }

        try {
            // Do request and get response
            String response = webClient.post()
                    .uri(fullUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Raw API Response: " + response);

            return extractResponseContent(response);

        } catch (WebClientResponseException e) {
            System.err.println("Gemini API Error (HTTP " + e.getRawStatusCode() + "): " + e.getResponseBodyAsString());
            return "Error from Gemini API: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            return "An unexpected error occurred: " + e.getMessage();
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            System.err.println("Error processing API response JSON: " + e.getMessage());
            return "Error processing response: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line. ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}