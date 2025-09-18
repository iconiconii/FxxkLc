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
        
        // Enhanced response format with JSON schema support and fallback
        Map<String, Object> responseFormat = buildResponseFormat();
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

            if (response.statusCode() != 200) {
                res.success = false;
                // Enhanced error logging with diagnostic details
                try {
                    String snippet = response.body() != null && response.body().length() > 500
                            ? response.body().substring(0, 500) + "..."
                            : String.valueOf(response.body());
                    log.warn("OpenAI provider HTTP {} for model {} at {}. Body: {}", 
                            response.statusCode(), cfg.getModel(), cfg.getBaseUrl(), snippet);
                    
                    // Enhanced error details for common issues
                    if (response.statusCode() == 400) {
                        if (snippet.contains("response_format") || snippet.contains("json_schema")) {
                            log.warn("Possible JSON schema incompatibility - provider may not support structured output");
                        } else if (snippet.contains("api_key") || snippet.contains("authentication")) {
                            log.warn("API key validation failed - check DEEPSEEK_API_KEY environment variable");
                        } else if (snippet.contains("model") || snippet.contains("not found")) {
                            log.warn("Model '{}' not found or unavailable", cfg.getModel());
                        }
                    }
                } catch (Exception ignore) {}
                res.error = classifyHttpError(response.statusCode(), response.body());
                res.items = List.of();
                res.latencyMs = (int) (System.currentTimeMillis() - start);
                return res;
            }

            // Parse OpenAI-compatible response with enhanced fallback
            ParseOutcome outcome = parseChatResponseWithFallback(response.body());
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
            res.error = classifyError(e);
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
        
        // Enhanced response format with JSON schema support and fallback
        Map<String, Object> responseFormat = buildResponseFormat();
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
                        out.error = classifyError(throwable);
                        out.items = List.of();
                        return out;
                    }
                    
                    if (resp.statusCode() != 200) {
                        out.success = false;
                        try {
                            String snippet = resp.body() != null && resp.body().length() > 500
                                    ? resp.body().substring(0, 500) + "..."
                                    : String.valueOf(resp.body());
                            log.warn("OpenAI provider HTTP {} for model {} at {}. Body: {}", 
                                    resp.statusCode(), cfg.getModel(), cfg.getBaseUrl(), snippet);
                            
                            // Enhanced error details for common issues
                            if (resp.statusCode() == 400) {
                                if (snippet.contains("response_format") || snippet.contains("json_schema")) {
                                    log.warn("Possible JSON schema incompatibility - provider may not support structured output");
                                } else if (snippet.contains("api_key") || snippet.contains("authentication")) {
                                    log.warn("API key validation failed - check DEEPSEEK_API_KEY environment variable");
                                } else if (snippet.contains("model") || snippet.contains("not found")) {
                                    log.warn("Model '{}' not found or unavailable", cfg.getModel());
                                }
                            }
                        } catch (Exception ignore) {}
                        out.error = classifyHttpError(resp.statusCode(), resp.body());
                        out.items = List.of();
                        return out;
                    }
                    
                    ParseOutcome outcome = parseChatResponseWithFallback(resp.body());
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

    /**
     * Builds response format with JSON schema support and fallback
     */
    private Map<String, Object> buildResponseFormat() {
        Map<String, Object> responseFormat = new HashMap<>();
        
        // Some OpenAI-compatible providers (e.g., DeepSeek) don't support json_schema yet.
        // For those, fall back to simple json_object to avoid 400 BAD_REQUEST.
        boolean likelyJsonSchemaUnsupported = false;
        try {
            String base = cfg.getBaseUrl() != null ? cfg.getBaseUrl().toLowerCase() : "";
            String model = cfg.getModel() != null ? cfg.getModel().toLowerCase() : "";
            if (base.contains("deepseek")) {
                likelyJsonSchemaUnsupported = true;
            }
            // Add other known providers here if needed
            if (model.contains("deepseek")) {
                likelyJsonSchemaUnsupported = true;
            }
        } catch (Exception ignore) {}
        
        if (likelyJsonSchemaUnsupported) {
            responseFormat.put("type", "json_object");
            return responseFormat;
        }

        // Try to use JSON schema format first (for better structured output)
        try {
            responseFormat.put("type", "json_schema");
            Map<String, Object> jsonSchema = new HashMap<>();
            jsonSchema.put("name", "recommendation_response");
            jsonSchema.put("strict", true);
            
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            
            Map<String, Object> properties = new HashMap<>();
            
            // Items array schema
            Map<String, Object> itemsSchema = new HashMap<>();
            itemsSchema.put("type", "array");
            Map<String, Object> itemSchema = new HashMap<>();
            itemSchema.put("type", "object");
            itemSchema.put("additionalProperties", false);
            Map<String, Object> itemProperties = new HashMap<>();
            itemProperties.put("problemId", Map.of("type", "integer"));
            itemProperties.put("score", Map.of("type", "number", "minimum", 0, "maximum", 1));
            itemProperties.put("reason", Map.of("type", "string", "minLength", 10, "maxLength", 500));
            itemProperties.put("confidence", Map.of("type", "number", "minimum", 0, "maximum", 1));
            itemSchema.put("properties", itemProperties);
            itemSchema.put("required", List.of("problemId", "score", "reason", "confidence"));
            itemsSchema.put("items", itemSchema);
            properties.put("items", itemsSchema);
            
            schema.put("properties", properties);
            schema.put("required", List.of("items"));
            
            jsonSchema.put("schema", schema);
            responseFormat.put("json_schema", jsonSchema);
            
            log.debug("Using JSON schema response format");
            return responseFormat;
            
        } catch (Exception e) {
            // Fallback to json_object format
            log.warn("Failed to build JSON schema format, falling back to json_object: {}", e.getMessage());
            responseFormat.clear();
            responseFormat.put("type", "json_object");
            return responseFormat;
        }
    }

    /**
     * Classifies error types for better debugging
     */
    private String classifyError(Throwable throwable) {
        if (throwable instanceof java.net.http.HttpTimeoutException) {
            return "TIMEOUT";
        } else if (throwable instanceof java.net.ConnectException) {
            return "CONNECTION_ERROR";
        } else if (throwable instanceof java.net.UnknownHostException) {
            return "DNS_ERROR";
        } else if (throwable instanceof java.io.IOException) {
            return "IO_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    /**
     * Classifies HTTP errors with specific handling for rate limits
     */
    private String classifyHttpError(int statusCode, String responseBody) {
        switch (statusCode) {
            case 400:
                return "BAD_REQUEST";
            case 401:
                return "UNAUTHORIZED";
            case 403:
                return "FORBIDDEN";
            case 429:
                return "RATE_LIMITED";
            case 500:
                return "INTERNAL_SERVER_ERROR";
            case 502:
                return "BAD_GATEWAY";
            case 503:
                return "SERVICE_UNAVAILABLE";
            case 504:
                return "GATEWAY_TIMEOUT";
            default:
                return "HTTP_" + statusCode;
        }
    }

    /**
     * Enhanced parsing with JSON schema validation fallback
     */
    private ParseOutcome parseChatResponseWithFallback(String responseBody) {
        try {
            // First attempt: standard parsing
            ParseOutcome outcome = parseChatResponse(responseBody);
            if (outcome.error == null) {
                return outcome;
            }
            
            // Second attempt: try alternative JSON extraction methods
            log.warn("Standard parsing failed with error: {}, attempting fallback parsing", outcome.error);
            return parseChatResponseFallback(responseBody);
            
        } catch (Exception e) {
            log.error("All parsing methods failed", e);
            ParseOutcome outcome = new ParseOutcome();
            outcome.error = "PARSING_ERROR";
            outcome.items = List.of();
            return outcome;
        }
    }

    /**
     * Fallback parsing method with more lenient JSON extraction
     */
    private ParseOutcome parseChatResponseFallback(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                ParseOutcome outcome = new ParseOutcome();
                outcome.error = "NO_CHOICES";
                outcome.items = List.of();
                return outcome;
            }

            JsonNode message = choices.get(0).get("message");
            if (message == null) {
                ParseOutcome outcome = new ParseOutcome();
                outcome.error = "NO_MESSAGE";
                outcome.items = List.of();
                return outcome;
            }

            String content = message.get("content").asText();
            
            // Try multiple JSON extraction approaches
            String[] jsonCandidates = {
                content,
                extractJson(content),
                extractJsonWithFallback(content)
            };
            
            for (String jsonCandidate : jsonCandidates) {
                if (jsonCandidate != null && !jsonCandidate.trim().isEmpty()) {
                    try {
                        List<RankedItem> items = tryParseItems(jsonCandidate);
                        if (items != null && !items.isEmpty()) {
                            ParseOutcome outcome = new ParseOutcome();
                            outcome.items = items;
                            outcome.error = null;
                            return outcome;
                        }
                    } catch (Exception e) {
                        log.debug("JSON parsing attempt failed: {}", e.getMessage());
                        continue;
                    }
                }
            }
            
            // If all attempts fail
            ParseOutcome outcome = new ParseOutcome();
            outcome.error = "PARSING_FAILED";
            outcome.items = List.of();
            return outcome;
            
        } catch (Exception e) {
            ParseOutcome outcome = new ParseOutcome();
            outcome.error = "JSON_PARSE_ERROR";
            outcome.items = List.of();
            return outcome;
        }
    }

    /**
     * Alternative JSON extraction with regex patterns
     */
    private String extractJsonWithFallback(String text) {
        if (text == null) return null;
        
        // Try various JSON extraction patterns
        String[] patterns = {
            "\\{.*?\"items\".*?\\}",
            "```json\\s*({.*?})\\s*```",
            "```\\s*({.*?})\\s*```",
            "({\\s*\"items\".*?})"
        };
        
        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, 
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = p.matcher(text);
                if (m.find()) {
                    String extracted = m.group(1);
                    if (extracted != null && extracted.trim().startsWith("{")) {
                        return extracted.trim();
                    }
                }
            } catch (Exception e) {
                log.debug("Pattern {} failed: {}", pattern, e.getMessage());
            }
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
