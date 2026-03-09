package com.emoque.service;

import com.emoque.client.OpenAiClient;
import com.emoque.dto.ChatImportRequest;
import com.emoque.model.ChatConversation;
import com.emoque.model.UserProfile;
import com.emoque.repository.ChatConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatImportService {

    private final OpenAiClient openAiClient;
    private final ChatConversationRepository chatConversationRepository;
    private final ObjectMapper objectMapper;
    private final ChatBufferService chatBufferService;

    public ChatImportService(OpenAiClient openAiClient,
                             ChatConversationRepository repository,
                             ObjectMapper objectMapper,
                             ChatBufferService chatBufferService) {
        this.openAiClient = openAiClient;
        this.chatConversationRepository = repository;
        this.objectMapper = objectMapper;
        this.chatBufferService = chatBufferService;
    }

    @Transactional
    public void importConversation(ChatImportRequest request) {
        List<String> messages = openAiClient.fetchConversation(
                request.openAiApiKey(),
                request.conversationId());
        persistConversation(request.userId(), messages);
        chatBufferService.store(request.userId(), String.join("\n", messages));
    }

    @Transactional
    public ChatConversation syncFromGoogleAccount(UserProfile profile) {
        // Deprecated: no longer storing chat data in DB. Keep a lightweight placeholder.
        String job = profile.getJob() == null ? "" : profile.getJob();
        String mbti = profile.getMbti() == null ? "" : profile.getMbti();
        List<String> messages = List.of(
                "Synced Google chat for " + profile.getName() + " (" + profile.getEmail() + ").",
                "Recent vibe check: enjoying " + (job.isBlank() ? "projects" : job) + ".",
                "Chatted about MBTI " + (mbti.isBlank() ? "curiosity" : mbti) + " and favorite moods.",
                "Shared personality keywords: " + String.join(", ", profile.getPersonalityKeywords())
        ).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(500)
                .collect(Collectors.toList());
        persistConversation(profile.getId(), messages);
        chatBufferService.store(profile.getId(), String.join("\n", messages));
        return new ChatConversation(profile.getId(), messages);
    }

    @Transactional
    public void importConversationFile(String userId, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String name = filename == null ? "" : filename.toLowerCase();
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            List<String> messages;
            if (name.endsWith(".json")) {
                messages = parseJsonMessages(content);
            } else if (name.endsWith(".html") || name.endsWith(".htm")) {
                messages = parseHtmlMessages(content);
            } else {
                messages = parsePlainMessages(content);
            }
            persistConversation(userId, messages);
            chatBufferService.store(userId, String.join("\n", messages));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read chat file: " + e.getMessage(), e);
        }
    }

    public String generateIntroFromBufferedChat(UserProfile profile) {
        String chatText = resolveChatText(profile.getId());
        return generateIntroFromText(profile, chatText);
    }

    public String generateIntroFromText(UserProfile profile, String chatText) {
        String safeChat = chatText == null ? "" : chatText;
        List<String> chunks = chunkText(safeChat, 4096, 16384);
        List<String> summaries = openAiClient.summarizeChunks(chunks);
        String mergedSummary = summaries.isEmpty() ? "" : String.join(" ", summaries);
        StringBuilder prompt = new StringBuilder();
        prompt.append("User: ").append(profile.getName()).append(" (").append(profile.getEmail()).append("). ");
        if (profile.getMbti() != null && !profile.getMbti().isBlank()) {
            prompt.append("MBTI: ").append(profile.getMbti()).append(". ");
        }
        if (profile.getJob() != null && !profile.getJob().isBlank()) {
            prompt.append("Job: ").append(profile.getJob()).append(". ");
        }
        if (profile.getPersonalityKeywords() != null && !profile.getPersonalityKeywords().isEmpty()) {
            prompt.append("Personality: ").append(String.join(", ", profile.getPersonalityKeywords())).append(". ");
        }
        if (!mergedSummary.isBlank()) {
            prompt.append("Conversation summary: ").append(mergedSummary).append(" ");
        } else {
            prompt.append("Conversation summary not provided; rely on profile only. ");
        }
        prompt.append("Write one punchy one-line introduction, 12 words max. No emojis or hashtags.");
        return openAiClient.generateBio(prompt.toString(), profile, null);
    }

    private List<String> chunkText(String text, int minSize, int maxSize) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int target = Math.max(minSize, Math.min(maxSize, 12000));
        List<String> chunks = new java.util.ArrayList<>();
        int len = text.length();
        int idx = 0;
        while (idx < len) {
            int end = Math.min(len, idx + target);
            chunks.add(text.substring(idx, end));
            idx = end;
        }
        return chunks;
    }

    private List<String> parsePlainMessages(String content) {
        return content.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(500)
                .collect(Collectors.toList());
    }

    private List<String> parseHtmlMessages(String content) {
        String stripped = content.replaceAll("<[^>]+>", " ");
        return parsePlainMessages(stripped);
    }

    private List<String> parseJsonMessages(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root.isArray()) {
                return parseJsonArray(root);
            }
            JsonNode msgs = root.path("messages");
            if (msgs.isArray()) {
                return parseJsonArray(msgs);
            }
        } catch (Exception ignored) {
        }
        return parsePlainMessages(content);
    }

    private List<String> parseJsonArray(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(node -> {
                    if (node.isTextual()) {
                        return node.asText();
                    }
                    JsonNode content = node.path("content");
                    if (content.isArray()) {
                        return java.util.stream.StreamSupport.stream(content.spliterator(), false)
                                .filter(JsonNode::isTextual)
                                .map(JsonNode::asText)
                                .collect(Collectors.joining(" "));
                    }
                    return content.isMissingNode() ? node.toString() : content.asText();
                })
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(500)
                .collect(Collectors.toList());
    }

    private String resolveChatText(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        ChatConversation conversation = chatConversationRepository.findByUserId(userId).orElse(null);
        if (conversation != null && conversation.getMessages() != null && !conversation.getMessages().isEmpty()) {
            return String.join("\n", conversation.getMessages());
        }
        return chatBufferService.get(userId);
    }

    private void persistConversation(String userId, List<String> messages) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        chatConversationRepository.save(new ChatConversation(userId, messages));
    }
}
