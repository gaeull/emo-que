package com.emoque.test;

import com.emoque.config.RedisConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import(RedisConfig.class)
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisSetGet() {
        redisTemplate.opsForValue().set("testKey", "Hello, Redis!");
        Object value = redisTemplate.opsForValue().get("testKey");
        assertThat(value).isEqualTo("Hello, Redis!");
    }

    @AfterAll
    static void cleanUp() {
        System.out.println("✅ Redis 테스트 완료");
    }
}
