package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import com.emoque.model.ChatConversation;
import com.emoque.model.IntroTask;
import com.emoque.model.UserProfile;
import com.emoque.repository.ChatConversationRepository;
import com.emoque.repository.IntroTaskRepository;
import com.emoque.repository.UserProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class IntroTaskService {

    private static final Logger log = LoggerFactory.getLogger(IntroTaskService.class);

    private final IntroTaskRepository introTaskRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatImportService chatImportService;
    private final RabbitTemplate rabbitTemplate;
    private final boolean queueEnabled;

    public IntroTaskService(IntroTaskRepository introTaskRepository,
                            UserProfileRepository userProfileRepository,
                            ChatConversationRepository chatConversationRepository,
                            ChatImportService chatImportService,
                            RabbitTemplate rabbitTemplate,
                            @Value("${intro.queue.enabled:true}") boolean queueEnabled) {
        this.introTaskRepository = introTaskRepository;
        this.userProfileRepository = userProfileRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatImportService = chatImportService;
        this.rabbitTemplate = rabbitTemplate;
        this.queueEnabled = queueEnabled;
    }

    @Transactional
    public IntroTask enqueue(String userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        IntroTask task = new IntroTask(UUID.randomUUID().toString(), profile.getId());
        introTaskRepository.save(task);

        if (queueEnabled) {
            publishAfterCommit(task.getId());
            return task;
        } else {
            log.info("Intro queue disabled; processing intro task {} inline", task.getId());
        }

        processTask(task.getId());
        return introTaskRepository.findById(task.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processTask(String taskId) {
        IntroTask task = findTaskWithRetry(taskId);
        if (task == null) {
            log.warn("Intro task {} not found after retry; skipping message", taskId);
            return;
        }
        task.setStatus(IntroTask.Status.RUNNING);
        introTaskRepository.save(task);

        try {
            UserProfile profile = userProfileRepository.findById(task.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            ChatConversation conversation = chatConversationRepository.findByUserId(task.getUserId())
                    .orElse(null);
            String chatText = conversation == null ? null : String.join("\n", conversation.getMessages());
            String intro = chatImportService.generateIntroFromText(profile, chatText);

            profile.setIntro(intro);
            userProfileRepository.save(profile);

            task.setIntro(intro);
            task.setStatus(IntroTask.Status.COMPLETED);
        } catch (Exception e) {
            task.setStatus(IntroTask.Status.FAILED);
            task.setFailureReason(e.getMessage());
        }
        introTaskRepository.save(task);
    }

    private IntroTask findTaskWithRetry(String taskId) {
        final int maxAttempts = 20;
        final long sleepMs = 100L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            IntroTask task = introTaskRepository.findById(taskId).orElse(null);
            if (task != null) {
                return task;
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }

    @Transactional
    public IntroTask getTask(String taskId) {
        IntroTask task = introTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));

        if (task.getStatus() == IntroTask.Status.QUEUED
                && task.getCreatedAt() != null
                && task.getCreatedAt().isBefore(Instant.now().minusSeconds(20))) {
            log.warn("Intro task {} stuck in QUEUED; attempting inline recovery", taskId);
            processTask(taskId);
            return introTaskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));
        }
        return task;
    }

    private void publishAfterCommit(String taskId) {
        Runnable action = () -> {
            try {
                log.info("Publishing intro task {} to RabbitMQ queue {}", taskId, RabbitMQConfig.INTRO_QUEUE);
                rabbitTemplate.convertAndSend(RabbitMQConfig.INTRO_QUEUE, taskId);
            } catch (Exception ex) {
                log.warn("RabbitMQ unavailable after commit, processing intro task {} inline", taskId, ex);
                processTask(taskId);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
