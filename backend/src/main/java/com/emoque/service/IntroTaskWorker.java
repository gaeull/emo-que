package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"worker.enabled", "intro.queue.enabled"}, havingValue = "true", matchIfMissing = true)
public class IntroTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(IntroTaskWorker.class);

    private final IntroTaskService introTaskService;

    public IntroTaskWorker(IntroTaskService introTaskService) {
        this.introTaskService = introTaskService;
    }

    @RabbitListener(queues = RabbitMQConfig.INTRO_QUEUE)
    public void handleIntroTask(String taskId) {
        log.info("Consumed intro task {} from RabbitMQ queue {}", taskId, RabbitMQConfig.INTRO_QUEUE);
        introTaskService.processTask(taskId);
    }
}
