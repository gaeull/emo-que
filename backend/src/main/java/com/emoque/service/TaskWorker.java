package com.emoque.service;

import com.emoque.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"worker.enabled", "generation.queue.enabled"}, havingValue = "true", matchIfMissing = true)
public class TaskWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskWorker.class);

    private final GenerationService generationService;

    public TaskWorker(GenerationService generationService) {
        this.generationService = generationService;
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void handleTask(String taskId) {
        log.info("Consumed generation task {} from RabbitMQ queue {}", taskId, RabbitMQConfig.TASK_QUEUE);
        generationService.processTask(taskId);
    }
}
