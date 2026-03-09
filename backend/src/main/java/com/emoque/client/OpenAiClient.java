package com.emoque.client;

import com.emoque.config.OpenAiProperties;
import com.emoque.model.ChatConversation;
import com.emoque.model.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {

    private final OpenAiProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public OpenAiClient(OpenAiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.http = HttpClient.newHttpClient();
    }

    private boolean isApiConfigured() {
        String key = properties.getApiKey();
        return key != null && !key.isBlank() && !"SET_ME".equalsIgnoreCase(key.trim());
    }

    public List<String> fetchConversation(String apiKey, String conversationId) {
        // OpenAI does not expose ChatGPT conversation fetch via public API.
        // Return empty list and rely on file upload flow.
        return List.of();
    }

    public String buildPrompt(UserProfile profile,
                              ChatConversation conversation,
                              List<String> emotions) {
        return buildPromptInternal(profile, conversation, emotions, true);
    }

    public String buildImagePrompt(UserProfile profile,
                                   ChatConversation conversation,
                                   List<String> emotions) {
        return buildPromptInternal(profile, conversation, emotions, false);
    }

    private String buildPromptInternal(UserProfile profile,
                                       ChatConversation conversation,
                                       List<String> emotions,
                                       boolean includeName) {
        String job = profile.getJob();
        StringBuilder builder = new StringBuilder();
        builder.append("Create emoticons for ");
        if (includeName) {
            builder.append(profile.getName());
        } else {
            builder.append("a person");
        }
        builder.append(job != null && !job.isBlank() ? ", a " + job.trim() : "")
                .append(" (MBTI: ")
                .append(profile.getMbti())
                .append(") focusing on emotions: ")
                .append(String.join(", ", emotions))
                .append(". Personality keywords: ")
                .append(String.join(", ", profile.getPersonalityKeywords()));

        if (job != null && !job.isBlank()) {
            builder.append(". Include subtle details that reference their job: ")
                    .append(job.trim());
        }

        builder
                .append(". Use format 1024x1024 PNG inspired by kakao emoticons, cute and simple.");
 //               .append(". Use format 1024x1024 PNG inspired by kakao emoticons, cute and simple and animation-like with thin outlines, soft gradients.");
 //               .append(". Use format 512x512 PNG inspired by LINE emoticons, super cute and animation-like with bold outlines, soft gradients, and simple solid backgrounds.");

        if (conversation != null) {
            builder.append(" Chat context: ")
                    .append(String.join(" | ", conversation.getMessages()));
        }
        return builder.toString();
    }

    public String generateBio(String prompt) {
        return generateBio(prompt, null, null);
    }

    public String generateBio(String prompt, UserProfile profile, ChatConversation conversation) {
        try {
            if ("ollama".equalsIgnoreCase(properties.getProvider())) {
                return generateViaOllama(prompt);
            }
            if (isApiConfigured()) {
                return generateViaOpenAi(prompt);
            }
            return localIntro(profile, conversation, prompt);
        } catch (Exception e) {
            if (profile != null || conversation != null) {
                return localIntro(profile, conversation, prompt);
            }
            throw new RuntimeException("Failed to generate bio via OpenAI", e);
        }
    }

    public List<String> summarizeChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<String> summaries = new java.util.ArrayList<>();
        for (String chunk : chunks) {
            if (chunk == null || chunk.isBlank()) continue;
            try {
                String summary = chat(
                        "You are a concise summarizer. Boil this chat excerpt down to one tight sentence capturing tone and key topics. 40 words max.",
                        chunk,
                        0.4,
                        120);
                if (summary != null && !summary.isBlank()) {
                    summaries.add(summary.trim());
                }
            } catch (Exception e) {
                // skip this chunk on failure
            }
        }
        return summaries;
    }

    private String chat(String system, String user, double temperature, int maxTokens) throws Exception {
        if ("ollama".equalsIgnoreCase(properties.getProvider())) {
            return chatViaOllama(system, user, temperature, maxTokens);
        }
        if (isApiConfigured()) {
            return chatViaOpenAi(system, user, temperature, maxTokens);
        }
        return localIntro(null, null, user);
    }

    private String generateViaOpenAi(String prompt) throws Exception {
        return chatViaOpenAi(
                "You write a single, punchy one-line personal intro. 12 words max. No emojis, no hashtags.",
                prompt,
                0.8,
                64);
    }

    private String generateViaOllama(String prompt) throws Exception {
        return chatViaOllama(
                "You write a single, punchy one-line personal intro. 12 words max. No emojis, no hashtags.",
                prompt,
                0.8,
                64);
    }

    public String getApiKey() {
        return properties.getApiKey();
    }

    private String localIntro(UserProfile profile, ChatConversation conversation, String prompt) {
        String name = profile != null && profile.getName() != null && !profile.getName().isBlank()
                ? profile.getName()
                : "Friendly creator";
        String job = profile != null && profile.getJob() != null && !profile.getJob().isBlank()
                ? profile.getJob()
                : "curious maker";
        String mbti = profile != null && profile.getMbti() != null && !profile.getMbti().isBlank()
                ? "(" + profile.getMbti() + ")"
                : "vibes-first";
        String keyword = (profile != null && profile.getPersonalityKeywords() != null && !profile.getPersonalityKeywords().isEmpty())
                ? profile.getPersonalityKeywords().get(0)
                : "bold energy";
        String convoHint = "";
        if (conversation != null && conversation.getMessages() != null && !conversation.getMessages().isEmpty()) {
            String first = conversation.getMessages().get(0);
            if (first != null && !first.isBlank()) {
                convoHint = " loves \"" + truncate(first, 24) + "\"";
            }
        }
        String sentence = (name + ", " + job + " " + mbti + " with " + keyword + " vibes" + convoHint).trim();
        // Cap at ~12 words to mimic OpenAI prompt behaviour.
        String[] words = sentence.split("\\s+");
        if (words.length > 12) {
            sentence = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 12));
        }
        return sentence;
    }

    private static String truncate(String text, int limit) {
        if (text == null) return "";
        return text.length() <= limit ? text : text.substring(0, limit).trim() + "…";
    }

    private String chatViaOpenAi(String system, String user, double temperature, int maxTokens) throws Exception {
        var bodyNode = mapper.createObjectNode();
        bodyNode.put("model", properties.getChatModel());
        var messages = mapper.createArrayNode();
        var sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", system);
        var usr = mapper.createObjectNode();
        usr.put("role", "user");
        usr.put("content", user);
        messages.add(sys);
        messages.add(usr);
        bodyNode.set("messages", messages);
        bodyNode.put("temperature", temperature);
        bodyNode.put("max_tokens", maxTokens);
        String body = bodyNode.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode()) {
                return content.asText().trim();
            }
        }
        throw new IllegalStateException("OpenAI chat completion failed: status=" + response.statusCode());
    }

    private String chatViaOllama(String system, String user, double temperature, int maxTokens) throws Exception {
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("model", properties.getChatModel());
        bodyNode.put("stream", false);
        var messages = mapper.createArrayNode();
        var sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", system);
        var usr = mapper.createObjectNode();
        usr.put("role", "user");
        usr.put("content", user);
        messages.add(sys);
        messages.add(usr);
        bodyNode.set("messages", messages);
        var options = mapper.createObjectNode();
        options.put("temperature", temperature);
        bodyNode.set("options", options);
        String body = bodyNode.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getOllamaUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("message").path("content");
            if (!content.isMissingNode()) {
                return content.asText().trim();
            }
        }
        throw new IllegalStateException("Ollama chat completion failed: status=" + response.statusCode());
    }
}
