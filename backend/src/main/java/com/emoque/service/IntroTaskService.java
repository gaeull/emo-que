package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import com.emoque.model.ChatConversation;
import com.emoque.model.IntroTask;
import com.emoque.model.UserProfile;
import com.emoque.repository.ChatConversationRepository;
import com.emoque.repository.IntroTaskRepository;
import com.emoque.repository.UserProfileRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.INTRO_QUEUE, task.getId());
                return task;
            } catch (Exception ex) {
                log.warn("RabbitMQ unavailable, processing intro task {} inline", task.getId(), ex);
            }
        }

        processTask(task.getId());
        return introTaskRepository.findById(task.getId())
                .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));
    }

    @Transactional
    public void processTask(String taskId) {
        IntroTask task = introTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));
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

    @Transactional(readOnly = true)
    public IntroTask getTask(String taskId) {
        return introTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Intro task not found"));
    }
}
