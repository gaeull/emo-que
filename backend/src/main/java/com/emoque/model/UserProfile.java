package com.emoque.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 64)
    private String gender;
    @Column(length = 128)
    private String job;
    @Column(length = 16)
    private String mbti;

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_personality_keywords", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "idx")
    @Column(name = "keyword")
    private List<String> personalityKeywords = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_emoticon_samples", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "idx")
    @Column(name = "url")
    private List<String> sampleEmoticonUrls = new ArrayList<>();

    public UserProfile() {
    }

    public UserProfile(String name,
                       String email,
                       String gender,
                       String job,
                       String mbti,
                       List<String> personalityKeywords,
                       List<String> sampleEmoticonUrls) {
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.job = job;
        this.mbti = mbti;
        if (personalityKeywords != null) {
            this.personalityKeywords = new ArrayList<>(personalityKeywords);
        }
        if (sampleEmoticonUrls != null) {
            this.sampleEmoticonUrls = new ArrayList<>(sampleEmoticonUrls);
        }
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getJob() {
        return job;
    }

    public void setMbti(String mbti) {
        this.mbti = mbti;
    }

    public String getMbti() {
        return mbti;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public List<String> getPersonalityKeywords() {
        return personalityKeywords;
    }

    public void setPersonalityKeywords(List<String> keywords) {
        this.personalityKeywords.clear();
        if (keywords != null) {
            this.personalityKeywords.addAll(keywords);
        }
    }

    public List<String> getSampleEmoticonUrls() {
        return sampleEmoticonUrls;
    }

    public void setSampleEmoticonUrls(List<String> samples) {
        this.sampleEmoticonUrls.clear();
        if (samples != null) {
            this.sampleEmoticonUrls.addAll(samples);
        }
    }
}
