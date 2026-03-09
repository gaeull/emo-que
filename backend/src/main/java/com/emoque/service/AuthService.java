package com.emoque.service;

import com.emoque.config.GoogleOAuthProperties;
import com.emoque.dto.LoginResponse;
import com.emoque.model.UserProfile;
import com.emoque.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GoogleOAuthProperties googleProps;
    private final UserProfileRepository users;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public AuthService(GoogleOAuthProperties googleProps,
                       UserProfileRepository users,
                       ObjectMapper mapper) {
        this.googleProps = googleProps;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public LoginResponse loginWithGoogleIdToken(String idToken) {
        GoogleToken tok = verifyIdToken(idToken);
        if (tok == null || tok.email() == null || tok.email().isBlank()) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        Optional<UserProfile> existing = users.findByEmail(tok.email());
        UserProfile profile = existing.orElseGet(() -> new UserProfile(
                tok.name() == null ? tok.email() : tok.name(),
                tok.email(),
                "",
                "",
                "",
                java.util.List.of("Creative"),
                java.util.List.of()
        ));
        if (existing.isEmpty()) {
            users.save(profile);
        }
        return new LoginResponse(profile.getId(), profile.getName(), profile.getEmail());
    }

    public String getGoogleClientId() {
        return googleProps.getId();
    }

    private GoogleToken verifyIdToken(String idToken) {
        try {
            // Use tokeninfo endpoint for simplicity
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + java.net.URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new IllegalArgumentException("Google token verification failed: status " + res.statusCode());
            }
            JsonNode root = mapper.readTree(res.body());
            String aud = text(root, "aud");
            String email = text(root, "email");
            String name = text(root, "name");
            String emailVerified = text(root, "email_verified");
            if (googleProps.getId() != null && !googleProps.getId().isBlank() && !googleProps.getId().equals(aud)) {
                throw new IllegalArgumentException("Google token audience mismatch");
            }
            if (email == null || !("true".equalsIgnoreCase(emailVerified) || "1".equals(emailVerified))) {
                throw new IllegalArgumentException("Email not verified");
            }
            return new GoogleToken(aud, email, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() ? null : v.asText(null);
    }

    private record GoogleToken(String aud, String email, String name) {}
}

