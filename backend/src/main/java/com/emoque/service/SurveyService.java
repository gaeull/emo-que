package com.emoque.service;

import com.emoque.dto.SurveyRequest;
import com.emoque.model.UserProfile;
import com.emoque.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyService {

    private final UserProfileRepository repository;

    public SurveyService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserProfile submitSurvey(SurveyRequest request) {
        UserProfile profile = repository.findByEmail(request.email())
                .orElseGet(() -> new UserProfile(
                        request.name(),
                        request.email(),
                        request.gender(),
                        request.job(),
                        request.mbti(),
                        request.personalityKeywords(),
                        request.sampleEmoticonUrls()));

        profile.setName(request.name());
        profile.setGender(request.gender());
        profile.setJob(request.job());
        profile.setMbti(request.mbti());
        profile.setPersonalityKeywords(request.personalityKeywords());
        profile.setSampleEmoticonUrls(request.sampleEmoticonUrls());
        return repository.save(profile);
    }
}
