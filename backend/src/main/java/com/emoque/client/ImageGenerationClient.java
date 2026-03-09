package com.emoque.client;

import com.emoque.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class ImageGenerationClient {

    private static final String STUB_BASE64_TRANSPARENT_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X2LZwAAAAASUVORK5CYII=";

    @Value("${image.provider:stub}")
    private String provider;

    @Value("${image.a1111.url:http://127.0.0.1:7860}")
    private String a1111Url;
    @Value("${image.a1111.model:nano-banana}")
    private String a1111Model;
    @Value("${image.openai.url}")
    private String geminiUrl;
    @Value("${image.openai.apiKey}")
    private String geminiApiKey;
    @Value("${image.size}")
    private String imageSize;

    private final OpenAiProperties openAi;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    public ImageGenerationClient(OpenAiProperties openAi, ObjectMapper mapper) {
        this.openAi = openAi;
        this.mapper = mapper;
    }

    public String generateImage(String prompt, String emotion) {
        String imagePrompt = prompt
                + " | Emotion: "
                + emotion
                + " | Create a simple character illustration. \\n" + //
                                        "\\n" + //
                                        "Style:\\n" + //
                                        "Minimalist illustration with thin, black line art.\\n" + //
                                        "simple shapes.\\n" + //
                                        "Flat colors.\\n" + //
                                        // "Soft, light pastel accents only when needed.\\n" + //
                                        " Overall feeling should be silly, awkward, and endearing rather than polished.\\n";
        // 2026-01-03 변경 전, 기본값
        // String imagePrompt = prompt
        //         + " | Emotion: "
        //         + emotion
        //         + " | Create a simple emoji-style character illustration. \\n" + //
        //                                 "\\n" + //
        //                                 "Style:\\n" + //
        //                                 "Minimalist illustration with thin, black line art.\\n" + //
        //                                 "simple shapes.\\n" + //
        //                                 "Flat colors or mostly white body, minimal shading.\\n" + //
        //                                 "Soft, light pastel accents only when needed.\\n" + //
        //                                 "Overall feeling should be silly, awkward, and endearing rather than polished.\\n" + //
        //                                 "\\n" + //
        //                                 "Character:\\n" + //
        //                                 "A small, animal-like character (dog / duck / cat-like, ambiguous is fine).\\n" + //
        //                                 "Overly simplified proportions with a slightly awkward posture.\\n";
        // 2026-01-02 변경 전, 기본값
        // String imagePrompt = prompt
        //         + " | Emotion: "
        //         + emotion
        //         // + " | Style: cute animated sticker, pastel background, bold outline, smooth shading, simple pose, no text.";
        //         //+ " | Style: cute but derpy meme sticker, overly pastel background, thick outline that looks like it tried to be stylish and failed, exaggerated sparkly shading, a random nonsensical pose, absolutely no text. Over-exaggerated facial features, awkward proportions, intentionally silly expression.";
        //         + " | Create a simple hand-drawn emoji-style character illustration. \\n" + //
        //                                 "\\n" + //
        //                                 "Style:\\n" + //
        //                                 "Minimalist doodle illustration with thin, slightly wobbly black line art.\\n" + //
        //                                 "Very simple shapes, childlike and intentionally imperfect.\\n" + //
        //                                 "Flat colors or mostly white body, minimal shading.\\n" + //
        //                                 "Soft, light pastel accents only when needed.\\n" + //
        //                                 "Overall feeling should be silly, awkward, and endearing rather than polished.\\n" + //
        //                                 "\\n" + //
        //                                 "Character:\\n" + //
        //                                 "A small, chubby, animal-like character (dog / duck / cat-like, ambiguous is fine).\\n" + //
        //                                 "Round head, tiny dot eyes, simple mouth.\\n" + //
        //                                 "Overly simplified proportions with a slightly awkward posture.\\n";
        try {
            if ("a1111".equalsIgnoreCase(provider)) {
                return generateWithA1111(imagePrompt);
            } else if ("openai".equalsIgnoreCase(provider)) {
                return generateWithOpenAi(imagePrompt);
            } else if ("gemini".equalsIgnoreCase(provider)) {
                return generateWithGemini(imagePrompt);
            }
        } catch (Exception ignored) {
            // Fall through to stub if local server not available
        }
        return STUB_BASE64_TRANSPARENT_PNG;
    }

    private String generateWithA1111(String prompt) throws Exception {
        String body = "{" +
                "\"prompt\":" + json(prompt) + "," +
                "\"steps\":20," +
                "\"width\":512," +
                "\"height\":512," +
                "\"override_settings\":{" +
                "\"sd_model_checkpoint\":" + json(a1111Model) +
                "}" +
                "}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(a1111Url + "/sdapi/v1/txt2img"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            String s = res.body();
            // naive parse: find first base64 in "images":["..."]
            int idx = s.indexOf("\"images\":");
            if (idx >= 0) {
                int start = s.indexOf('"', idx + 9);
                start = s.indexOf('"', start + 1); // first quote inside array
                int end = s.indexOf('"', start + 1);
                if (start >= 0 && end > start) {
                    String b64 = s.substring(start + 1, end);
                    return b64;
                }
            }
        }
        throw new IllegalStateException("A1111 generation failed: " + res.statusCode());
    }

    private String generateWithOpenAi(String prompt) throws Exception {
        String model = (openAi.getImageModel() == null || openAi.getImageModel().isBlank())
                ? "gpt-image-1"
                : openAi.getImageModel();
        String size = normalizeOpenAiSize(model, imageSize);
        String body = mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("size", size)
                .put("n", 1)
                //.put("response_format", "b64_json") --> unknown parameter
                .toString();
        HttpRequest req = HttpRequest.newBuilder()
                // .uri(URI.create("https://api.openai.com/v1/images/generations"))
                .uri(URI.create(geminiUrl))
                .header("Authorization", "Bearer " + geminiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("OpenAI image status = " + res.statusCode());
        System.out.println("OpenAI image body   = " + res.body());
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            JsonNode root = mapper.readTree(res.body());
            JsonNode b64 = root.path("data").path(0).path("b64_json");
            if (!b64.isMissingNode() && !b64.asText().isEmpty()) {
                return b64.asText();
            }
        }
        throw new IllegalStateException("OpenAI image generation failed: " + res.statusCode());
    }

    // private String generateWithGemini(String prompt) throws Exception {
    //     if (geminiApiKey == null || geminiApiKey.isBlank()) {
    //         throw new IllegalStateException("Gemini API key is missing");
    //     }
    //     int[] size = parseSize(imageSize);
    //     var root = mapper.createObjectNode();
    //     var contents = mapper.createArrayNode();
    //     var content = mapper.createObjectNode();
    //     var parts = mapper.createArrayNode();
    //     var part = mapper.createObjectNode();
    //     part.put("text", prompt);
    //     parts.add(part);
    //     content.set("parts", parts);
    //     contents.add(content);
    //     root.set("contents", contents);
    //     var genCfg = mapper.createObjectNode();
    //     genCfg.put("responseMimeType", "image/png");
    //     var dims = mapper.createObjectNode();
    //     dims.put("width", size[0]);
    //     dims.put("height", size[1]);
    //     genCfg.set("responseImageDimensions", dims);
    //     root.set("generationConfig", genCfg);

    //     HttpRequest req = HttpRequest.newBuilder()
    //             .uri(URI.create(geminiUrl + "?key=" + geminiApiKey))
    //             .header("Content-Type", "application/json")
    //             .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
    //             .build();
    //     HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
    //     if (res.statusCode() >= 200 && res.statusCode() < 300) {
    //         JsonNode rootNode = mapper.readTree(res.body());
    //         JsonNode data = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("inlineData").path("data");
    //         if (!data.isMissingNode() && !data.asText().isBlank()) {
    //             return "data:image/png;base64," + data.asText();
    //         }
    //     }
    //     throw new IllegalStateException("Gemini image generation failed: " + res.statusCode());
    // }

    private String generateWithGemini(String prompt) throws Exception {

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing");
        }

        int[] size = parseSize(imageSize);
        var root = mapper.createObjectNode();

        // prompt
        var contents = mapper.createArrayNode();
        var content = mapper.createObjectNode();
        content.put("role", "user");
        var parts = mapper.createArrayNode();
        var part = mapper.createObjectNode();
        part.put("text", prompt);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        root.set("contents", contents);

        // generationConfig
        var genCfg = mapper.createObjectNode();

        // 이미지 응답을 원하면 responseModalities 사용
        var modalities = mapper.createArrayNode();
        modalities.add("Image");
        genCfg.set("responseModalities", modalities);

        // genCfg.put("responseMimeType", "image/png");
        // 2026-01-03 gemini api에 지원하지 않는 스펙(responseImageDimensions)
        // var dims = mapper.createObjectNode();
        // dims.put("width", size[0]);
        // dims.put("height", size[1]);
        // genCfg.set("responseImageDimensions", dims);
        root.set("generationConfig", genCfg);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(geminiUrl + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        // 2026-01-03 add debug point
        System.out.println("response status code: " + res.statusCode());
        System.out.println("response status body: " + res.body());

        // 여기서 throw발생
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            JsonNode rootNode = mapper.readTree(res.body());
            JsonNode data = rootNode
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("inlineData").path("data");

            if (!data.isMissingNode() && !data.asText().isBlank()) {
                return data.asText();
            }
        }
        throw new IllegalStateException("Gemini image generation failed: " + res.statusCode());
    }

    private String normalizeOpenAiSize(String model, String requestedSize) {
        if (requestedSize == null || requestedSize.isBlank()) {
            return "1024x1024";
        }
        String normalized = requestedSize.toLowerCase().trim();
        boolean isOpenAiModel = model != null && model.toLowerCase().matches(".*(gpt-image|dall-e).*");
        if (isOpenAiModel) {
            // OpenAI image models accept only 1024 or 1792 dimensions; clamp invalid values.
            if (!normalized.equals("1024x1024")
                    && !normalized.equals("1792x1024")
                    && !normalized.equals("1024x1792")) {
                return "1024x1024";
            }
        }
        return normalized;
    }

    private int[] parseSize(String size) {
        try {
            String[] parts = size.toLowerCase().split("x");
            int w = Integer.parseInt(parts[0].trim());
            int h = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : w;
            return new int[]{w, h};
        } catch (Exception e) {
            return new int[]{512, 512};
        }
    }

    private static String json(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}


