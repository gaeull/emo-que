package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class TaskWorker {

    private final GenerationService generationService;

    public TaskWorker(GenerationService generationService) {
        this.generationService = generationService;
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void handleTask(String taskId) {
        generationService.processTask(taskId);
    }
}
