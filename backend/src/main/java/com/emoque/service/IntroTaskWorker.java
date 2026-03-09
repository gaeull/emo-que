package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class IntroTaskWorker {

    private final IntroTaskService introTaskService;

    public IntroTaskWorker(IntroTaskService introTaskService) {
        this.introTaskService = introTaskService;
    }

    @RabbitListener(queues = RabbitMQConfig.INTRO_QUEUE)
    public void handleIntroTask(String taskId) {
        introTaskService.processTask(taskId);
    }
}
