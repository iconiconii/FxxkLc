package com.codetop.recommendation.provider.impl;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.RequestContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Minimal OpenAI provider stub using JDK HttpClient.
 * Makes a safe placeholder call only when API key is present.
 */
public class OpenAiProvider implements LlmProvider {
    private final HttpClient httpClient;
    private final LlmProperties.OpenAi cfg;

    public OpenAiProvider(LlmProperties.OpenAi cfg) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.cfg = cfg;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public LlmResult rankCandidates(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        long start = System.currentTimeMillis();
        LlmResult res = new LlmResult();
        res.provider = name();
        res.model = cfg.getModel();

        String apiKey = System.getenv(cfg.getApiKeyEnv());
        if (apiKey == null || apiKey.isEmpty()) {
            res.success = false;
            res.error = "API_KEY_MISSING";
            res.items = List.of();
            res.latencyMs = (int) (System.currentTimeMillis() - start);
            return res;
        }

        String payload = "{\n" +
                "  \"model\": \"" + cfg.getModel() + "\",\n" +
                "  \"messages\": [{\n" +
                "    \"role\": \"user\",\n" +
                "    \"content\": \"Return empty JSON: {\\\"items\\\": []}\"\n" +
                "  }],\n" +
                "  \"temperature\": 0\n" +
                "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(cfg.getTimeoutMs() != null ? cfg.getTimeoutMs() : 1800))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            res.success = response.statusCode() >= 200 && response.statusCode() < 300;
            res.items = List.of();
            res.latencyMs = (int) (System.currentTimeMillis() - start);
            return res;
        } catch (Exception e) {
            res.success = false;
            res.error = e.getClass().getSimpleName();
            res.items = List.of();
            res.latencyMs = (int) (System.currentTimeMillis() - start);
            return res;
        }
    }
}
