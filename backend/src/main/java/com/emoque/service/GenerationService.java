package com.emoque.service;

import com.emoque.client.ImageGenerationClient;
import com.emoque.client.OpenAiClient;
import com.emoque.config.RabbitMQConfig;
import com.emoque.dto.GenerationRequest;
import com.emoque.dto.GenerationResponse;
import com.emoque.dto.TaskStatusResponse;
import com.emoque.model.ChatConversation;
import com.emoque.model.GenerationTask;
import com.emoque.model.UserProfile;
import com.emoque.repository.ChatConversationRepository;
import com.emoque.repository.GenerationTaskRepository;
import com.emoque.repository.UserProfileRepository;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private final UserProfileRepository userProfileRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final OpenAiClient openAiClient;
    private final ImageGenerationClient imageGenerationClient;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final boolean queueEnabled;
    private final boolean redisEnabled;

    public GenerationService(UserProfileRepository userProfileRepository,
                             ChatConversationRepository chatConversationRepository,
                             GenerationTaskRepository generationTaskRepository,
                             OpenAiClient openAiClient,
                             ImageGenerationClient imageGenerationClient,
                             RabbitTemplate rabbitTemplate,
                             StringRedisTemplate redisTemplate,
                             @Value("${generation.queue.enabled:true}") boolean queueEnabled,
                             @Value("${generation.redis.enabled:false}") boolean redisEnabled) {
        this.userProfileRepository = userProfileRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.openAiClient = openAiClient;
        this.imageGenerationClient = imageGenerationClient;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
        this.queueEnabled = queueEnabled;
        this.redisEnabled = redisEnabled;
    }

    @Transactional
    public TaskStatusResponse enqueueGeneration(GenerationRequest request) {
        UserProfile profile = userProfileRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

        String taskId = UUID.randomUUID().toString();
        GenerationTask task = new GenerationTask(taskId, profile.getId(), request.emotions());
        generationTaskRepository.save(task);

        if (queueEnabled) {
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.TASK_QUEUE, taskId);
                setStatusInRedis(taskId, GenerationTask.Status.QUEUED);
                return new TaskStatusResponse(taskId, task.getStatus().name(), null);
            } catch (Exception ex) {
                log.warn("RabbitMQ unavailable, processing task {} inline", taskId, ex);
            }
        }

        // Fallback: process inline when RabbitMQ is disabled/unavailable
        task.setStatus(GenerationTask.Status.RUNNING);
        generationTaskRepository.save(task);
        setStatusInRedis(taskId, GenerationTask.Status.RUNNING);

        processTask(taskId);

        GenerationTask updatedTask = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        return new TaskStatusResponse(updatedTask.getId(), updatedTask.getStatus().name(), updatedTask.getFailureReason());
    }

    @Transactional
    public void processTask(String taskId) {
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        task.setStatus(GenerationTask.Status.RUNNING);
        setStatusInRedis(taskId, GenerationTask.Status.RUNNING);

        try {
            UserProfile profile = userProfileRepository.findById(task.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Missing user profile"));
            ChatConversation conversation = chatConversationRepository.findByUserId(task.getUserId())
                    .orElse(null);

            String bioPrompt = openAiClient.buildPrompt(profile, conversation, task.getEmotions());
            String imagePrompt = openAiClient.buildImagePrompt(profile, conversation, task.getEmotions());
            String bio = openAiClient.generateBio(bioPrompt, profile, conversation);
            task.setBio(bio);

            for (String emotion : task.getEmotions()) {
                String imageUrl = imageGenerationClient.generateImage(imagePrompt, emotion);
                task.getEmotionImageUrls().put(emotion, imageUrl);
            }

            task.setStatus(GenerationTask.Status.COMPLETED);
            setStatusInRedis(taskId, GenerationTask.Status.COMPLETED);
        } catch (Exception e) {
            task.setStatus(GenerationTask.Status.FAILED);
            task.setFailureReason(e.getMessage());
            setStatusInRedis(taskId, GenerationTask.Status.FAILED);
        }
        generationTaskRepository.save(task);
    }

    private void setStatusInRedis(String taskId, GenerationTask.Status status) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(taskId, status.name());
        } catch (Exception ex) {
            log.warn("Redis unavailable, skipping status cache for task {}", taskId, ex);
        }
    }

    @Transactional(readOnly = true)
    public GenerationResponse getTask(String taskId) {
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        List<String> downloads = new ArrayList<>();
        if (!task.getEmotionImageUrls().isEmpty()) {
            downloads.add("/api/generation/" + taskId + "/download");
        }

        Map<String, String> copy = new HashMap<>(task.getEmotionImageUrls());
        return new GenerationResponse(
                task.getId(),
                task.getStatus().name(),
                task.getBio(),
                copy,
                downloads);
    }

    @Transactional(readOnly = true)
    public byte[] buildDownloadZip(String taskId) {
        return buildDownloadZip(taskId, null);
    }

    @Transactional(readOnly = true)
    public byte[] buildDownloadZip(String taskId, List<String> overrideImages) {
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != GenerationTask.Status.COMPLETED
                && (overrideImages == null || overrideImages.isEmpty())) {
            return new byte[0];
        }

        List<String> base64Images = resolveImages(task, overrideImages);
        if (base64Images.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < base64Images.size(); i++) {
                String raw = base64Images.get(i);
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    String clean = stripDataPrefix(raw);
                    byte[] imageBytes = Base64.getDecoder().decode(clean);

                    String filename = String.format("emoji_%02d.png", i + 1);
                    zos.putNextEntry(new ZipEntry(filename));
                    zos.write(imageBytes);
                    zos.closeEntry();
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid base64 image at index {} for task {}", i, taskId);
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to build zip for task {}", taskId, e);
            return new byte[0];
        }
    }

    private List<String> resolveImages(GenerationTask task, List<String> overrideImages) {
        if (overrideImages != null && !overrideImages.isEmpty()) {
            return overrideImages;
        }
        Map<String, String> map = task.getEmotionImageUrls();
        if (map.isEmpty()) {
            return List.of();
        }
        List<String> ordered = new ArrayList<>();
        if (task.getEmotions() != null && !task.getEmotions().isEmpty()) {
            for (String emotion : task.getEmotions()) {
                String v = map.get(emotion);
                if (v != null && !v.isBlank()) {
                    ordered.add(v);
                }
            }
        }
        if (ordered.isEmpty()) {
            ordered.addAll(map.values());
        }
        return ordered;
    }

    private String stripDataPrefix(String raw) {
        if (raw == null) return "";
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            return raw.substring(comma + 1);
        }
        return raw;
    }
}
