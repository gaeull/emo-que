package com.emoque.controller;

import com.emoque.dto.SurveyResponse;
import com.emoque.model.UserProfile;
import com.emoque.repository.UserProfileRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Validated
public class UsersController {

    public record InitUserRequest(@NotEmpty String name, @Email @NotEmpty String email) {}

    private final UserProfileRepository repository;

    public UsersController(UserProfileRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/init")
    public ResponseEntity<SurveyResponse> init(@RequestBody InitUserRequest req) {
        UserProfile profile = new UserProfile(
                req.name(),
                req.email(),
                "",
                "",
                "",
                java.util.List.of("Creative"),
                java.util.List.of()
        );
        repository.save(profile);
        return ResponseEntity.ok(new SurveyResponse(profile.getId()));
    }
}

