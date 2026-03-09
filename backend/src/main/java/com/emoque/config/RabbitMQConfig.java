package com.emoque.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String TASK_QUEUE = "emoque.tasks";
    public static final String INTRO_QUEUE = "emoque.intro";

    @Bean
    public Queue taskQueue() {
        return new Queue(TASK_QUEUE, true);
    }

    @Bean
    public Queue introQueue() {
        return new Queue(INTRO_QUEUE, true);
    }
}
