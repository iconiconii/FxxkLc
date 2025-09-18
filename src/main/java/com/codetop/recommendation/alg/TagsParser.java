package com.codetop.recommendation.alg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Collections;

/**
 * Utility class for parsing problem tags from JSON strings.
 * Handles JSON parsing errors gracefully and returns empty list on failure.
 */
@Component
@Slf4j
public class TagsParser {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeReference<List<String>> listTypeRef = new TypeReference<List<String>>() {};

    /**
     * Parse tags from JSON string to List<String>.
     * @param tagsJson JSON string like ["array", "two-pointers"] 
     * @return List of tag strings, empty list if parsing fails
     */
    public List<String> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<String> tags = objectMapper.readValue(tagsJson, listTypeRef);
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            log.debug("Failed to parse tags JSON '{}': {}", tagsJson, e.getMessage());
            return Collections.emptyList();
        }
    }
}