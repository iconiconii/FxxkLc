package com.codetop.recommendation.provider.impl;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.service.PromptTemplateService;
import com.codetop.recommendation.service.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible provider used for DeepSeek via baseUrl/model.
 * - Builds a structured JSON-only prompt using PromptTemplateService
 * - Requests JSON response_format when supported
 * - Parses assistant content to extract { items: [...] }
 */
public class OpenAiProvider implements LlmProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);
    private final HttpClient httpClient;
    private final LlmProperties.OpenAi cfg;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptTemplateService promptTemplateService;

    public OpenAiProvider(LlmProperties.OpenAi cfg, PromptTemplateService promptTemplateService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.cfg = cfg;
        this.promptTemplateService = promptTemplateService;
    }

    // Backward compatible constructor for tests
    public OpenAiProvider(LlmProperties.OpenAi cfg) {
        this(cfg, null);
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

        String apiKey = resolveApiKey(cfg.getApiKeyEnv());
        if (apiKey == null || apiKey.isEmpty()) {
            res.success = false;
            res.error = "API_KEY_MISSING";
            res.items = List.of();
            res.latencyMs = (int) (System.currentTimeMillis() - start);
            return res;
        }

        // Build messages using prompt template service
        String promptVersion = promptTemplateService != null ? promptTemplateService.getCurrentPromptVersion() : "v1";
        String systemMsg = promptTemplateService != null ? 
            promptTemplateService.buildSystemMessage(promptVersion) :
            buildFallbackSystemMessage();
        String userMsg = promptTemplateService != null ?
            promptTemplateService.buildUserMessage(ctx, candidates, options, promptVersion) :
            buildFallbackUserMessage(ctx, candidates, options);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", cfg.getModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemMsg));
        messages.add(Map.of("role", "user", "content", userMsg));
        payload.put("messages", messages);
        payload.put("temperature", 0);
        // Prefer JSON object format if supported by provider (DeepSeek compatible with OpenAI style)
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        payload.put("response_format", responseFormat);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(cfg.getTimeoutMs() != null ? cfg.getTimeoutMs() : 1800))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                res.success = false;
                res.error = "HTTP_" + response.statusCode();
                res.items = List.of();
                res.latencyMs = (int) (System.currentTimeMillis() - start);
                return res;
            }

            // Parse OpenAI-compatible response
            ParseOutcome outcome = parseChatResponse(response.body());
            if (outcome.error != null) {
                res.success = false;
                res.error = outcome.error;
                res.items = List.of();
            } else {
                res.success = true;
                res.items = outcome.items != null ? outcome.items : List.of();
            }
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

    @Override
    public java.util.concurrent.CompletableFuture<LlmResult> rankCandidatesAsync(RequestContext ctx,
                                                                                 List<ProblemCandidate> candidates,
                                                                                 PromptOptions options) {
        long start = System.currentTimeMillis();
        String apiKey = resolveApiKey(cfg.getApiKeyEnv());

        // Early return if missing API key
        if (apiKey == null || apiKey.isEmpty()) {
            LlmResult early = new LlmResult();
            early.provider = name();
            early.model = cfg.getModel();
            early.success = false;
            early.error = "API_KEY_MISSING";
            early.items = List.of();
            early.latencyMs = (int) (System.currentTimeMillis() - start);
            return java.util.concurrent.CompletableFuture.completedFuture(early);
        }

        // Build request payload using prompt template service
        String promptVersion = promptTemplateService != null ? promptTemplateService.getCurrentPromptVersion() : "v1";
        String systemMsg = promptTemplateService != null ? 
            promptTemplateService.buildSystemMessage(promptVersion) :
            buildFallbackSystemMessage();
        String userMsg = promptTemplateService != null ?
            promptTemplateService.buildUserMessage(ctx, candidates, options, promptVersion) :
            buildFallbackUserMessage(ctx, candidates, options);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", cfg.getModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemMsg));
        messages.add(Map.of("role", "user", "content", userMsg));
        payload.put("messages", messages);
        payload.put("temperature", 0);
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        payload.put("response_format", responseFormat);

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            LlmResult err = new LlmResult();
            err.provider = name();
            err.model = cfg.getModel();
            err.success = false;
            err.error = "PAYLOAD_BUILD_ERROR";
            err.items = List.of();
            err.latencyMs = (int) (System.currentTimeMillis() - start);
            return java.util.concurrent.CompletableFuture.completedFuture(err);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cfg.getBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMillis(cfg.getTimeoutMs() != null ? cfg.getTimeoutMs() : 1800))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((resp, throwable) -> {
                    LlmResult out = new LlmResult();
                    out.provider = name();
                    out.model = cfg.getModel();
                    out.latencyMs = (int) (System.currentTimeMillis() - start);
                    if (throwable != null) {
                        out.success = false;
                        out.error = throwable.getClass().getSimpleName();
                        out.items = List.of();
                        return out;
                    }
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        out.success = false;
                        out.error = "HTTP_" + resp.statusCode();
                        out.items = List.of();
                        return out;
                    }
                    ParseOutcome outcome = parseChatResponse(resp.body());
                    if (outcome.error != null) {
                        out.success = false;
                        out.error = outcome.error;
                        out.items = List.of();
                    } else {
                        out.success = true;
                        out.items = outcome.items != null ? outcome.items : List.of();
                    }
                    return out;
                });
    }

    private String resolveApiKey(String apiKeyEnvOrValue) {
        if (apiKeyEnvOrValue == null || apiKeyEnvOrValue.isEmpty()) return null;
        // If looks like a secret key itself, accept literal for backward compatibility
        String v = apiKeyEnvOrValue.trim();
        if (v.startsWith("sk-")) {
            // Warn if a literal API key is provided instead of environment variable name
            log.warn("OpenAI provider received a literal API key. It is recommended to use environment variable names (e.g., OPENAI_API_KEY).");
            return v;
        }
        return System.getenv(v);
    }

    // Fallback methods for when PromptTemplateService is not available (backward compatibility)
    private String buildFallbackSystemMessage() {
        return "You are a recommendation re-ranking engine for algorithm problems. " +
                "Return STRICT JSON only with schema: {\"items\": [{\"problemId\": number, \"reason\": string, " +
                "\"confidence\": number, \"strategy\": string, \"score\": number}]} . " +
                "No markdown, no explanations, no extra fields.";
    }

    private String buildFallbackUserMessage(RequestContext ctx, List<ProblemCandidate> candidates, PromptOptions options) {
        int limit = options != null ? Math.max(1, Math.min(50, options.limit)) : 10;
        StringBuilder sb = new StringBuilder();
        sb.append("Task: Rank the candidates and return top ").append(limit).append(" items.\\n");
        sb.append("User: ").append(ctx != null && ctx.getUserId() != null ? ctx.getUserId() : "unknown").append("\\n");
        sb.append("Route: ").append(ctx != null && ctx.getRoute() != null ? ctx.getRoute() : "ai-recommendations").append("\\n");
        sb.append("Constraints: Output strictly as JSON object with field 'items'.\\n");
        sb.append("Candidates (array of objects with minimal fields): ");
        try {
            List<Map<String, Object>> arr = new ArrayList<>();
            if (candidates != null) {
                for (ProblemCandidate c : candidates) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.id);
                    if (c.topic != null) m.put("topic", c.topic);
                    if (c.difficulty != null) m.put("difficulty", c.difficulty);
                    if (c.tags != null) m.put("tags", c.tags);
                    if (c.recentAccuracy != null) m.put("recentAccuracy", c.recentAccuracy);
                    if (c.attempts != null) m.put("attempts", c.attempts);
                    arr.add(m);
                }
            }
            sb.append(objectMapper.writeValueAsString(arr));
        } catch (JsonProcessingException e) {
            sb.append("[]");
        }
        sb.append("\\nReturn only JSON. Example: {\"items\":[{\"problemId\":1,\"reason\":\"...\",\"confidence\":0.8,\"strategy\":\"progressive\",\"score\":0.8}]} ");
        return sb.toString();
    }

    private ParseOutcome parseChatResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return ParseOutcome.error("NO_CHOICES");
            }
            JsonNode msg = choices.get(0).path("message");
            String content = msg.path("content").asText("");
            if (content.isEmpty()) {
                return ParseOutcome.error("EMPTY_CONTENT");
            }

            // Try content as JSON directly
            List<RankedItem> items = tryParseItems(content);
            if (items == null) {
                // Try to extract JSON section (e.g., fenced code)
                String extracted = extractJson(content);
                if (extracted != null) {
                    items = tryParseItems(extracted);
                }
            }

            if (items == null) {
                return ParseOutcome.error("PARSING_ERROR");
            }
            return ParseOutcome.ok(items);
        } catch (Exception e) {
            return ParseOutcome.error("PARSING_EXCEPTION:" + e.getClass().getSimpleName());
        }
    }

    private List<RankedItem> tryParseItems(String content) {
        try {
            JsonNode node = objectMapper.readTree(content);
            JsonNode itemsNode = node.path("items");
            if (!itemsNode.isArray()) return null;
            List<RankedItem> out = new ArrayList<>();
            for (JsonNode it : itemsNode) {
                RankedItem r = new RankedItem();
                if (!it.has("problemId")) continue;
                r.problemId = it.path("problemId").isIntegralNumber() ? it.path("problemId").asLong() : null;
                if (r.problemId == null) continue;
                r.reason = it.path("reason").asText(null);
                r.confidence = it.path("confidence").isNumber() ? it.path("confidence").asDouble() : 0.0;
                r.strategy = it.path("strategy").asText(null);
                r.score = it.path("score").isNumber() ? it.path("score").asDouble() : r.confidence;
                out.add(r);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJson(String content) {
        // Try code fence first
        int startFence = content.indexOf("```json");
        if (startFence >= 0) {
            int start = startFence + 7; // after ```json
            int endFence = content.indexOf("```", start);
            if (endFence > start) {
                return content.substring(start, endFence).trim();
            }
        }
        // Fallback: naive first { ... last }
        int first = content.indexOf('{');
        int last = content.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return content.substring(first, last + 1).trim();
        }
        return null;
    }

    private static class ParseOutcome {
        List<RankedItem> items;
        String error;
        static ParseOutcome ok(List<RankedItem> items) { ParseOutcome o = new ParseOutcome(); o.items = items; return o; }
        static ParseOutcome error(String err) { ParseOutcome o = new ParseOutcome(); o.error = err; return o; }
    }
}
