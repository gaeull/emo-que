package com.emoque.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ChatBufferService {

    private final Map<String, String> buffer = new ConcurrentHashMap<>();

    public void store(String userId, String content) {
        if (userId == null) return;
        buffer.put(userId, content == null ? "" : content);
    }

    public String get(String userId) {
        return userId == null ? null : buffer.get(userId);
    }

    public void clear(String userId) {
        if (userId != null) {
            buffer.remove(userId);
        }
    }
}
