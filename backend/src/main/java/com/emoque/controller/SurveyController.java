package com.emoque.controller;

import com.emoque.dto.SurveyRequest;
import com.emoque.dto.SurveyResponse;
import com.emoque.model.UserProfile;
import com.emoque.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey")
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping
    public ResponseEntity<SurveyResponse> submitSurvey(@Valid @RequestBody SurveyRequest request) {
        UserProfile profile = surveyService.submitSurvey(request);
        return ResponseEntity.ok(new SurveyResponse(profile.getId()));
    }
}
