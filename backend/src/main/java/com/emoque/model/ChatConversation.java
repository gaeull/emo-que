package com.emoque.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    private String userId;

    @ElementCollection
    @CollectionTable(name = "chat_messages", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "idx")
    @Column(name = "message", columnDefinition = "TEXT")
    private List<String> messages = new ArrayList<>();

    @Column(name = "imported_at")
    private Instant importedAt;

    public ChatConversation() {
    }

    public ChatConversation(String userId, List<String> messages) {
        this.userId = userId;
        if (messages != null) {
            this.messages = new ArrayList<>(messages);
        }
    }

    @PrePersist
    public void prePersist() {
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
