package com.emoque.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "generation_tasks")
public class GenerationTask {

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.QUEUED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ElementCollection
    @CollectionTable(name = "generation_task_emotions", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "emotion")
    private List<String> emotions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "generation_task_images", joinColumns = @JoinColumn(name = "task_id"))
    @MapKeyColumn(name = "emotion")
    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private Map<String, String> emotionImageUrls = new HashMap<>();

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public GenerationTask() {
    }

    public GenerationTask(String id, String userId, List<String> emotions) {
        this.id = id;
        this.userId = userId;
        this.emotions = new ArrayList<>(emotions);
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.COMPLETED || status == Status.FAILED) {
            this.completedAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<String> getEmotions() {
        return emotions;
    }

    public Map<String, String> getEmotionImageUrls() {
        return emotionImageUrls;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
