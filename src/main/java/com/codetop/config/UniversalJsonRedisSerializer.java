package com.codetop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * Universal JSON Redis Serializer that handles complex objects properly.
 * 
 * This serializer addresses ClassCastException issues with GenericJackson2JsonRedisSerializer
 * by using proper type information handling for complex entities like User.
 * 
 * @author CodeTop Team
 */
@Slf4j
public class UniversalJsonRedisSerializer implements RedisSerializer<Object> {
    
    private final ObjectMapper objectMapper;
    
    public UniversalJsonRedisSerializer() {
        this.objectMapper = new ObjectMapper();
        
        // Configure ObjectMapper for proper serialization
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        // Enable default typing for proper type handling - this is crucial for avoiding ClassCastException
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        log.info("UniversalJsonRedisSerializer initialized with enhanced type information handling");
    }
    
    @Override
    public byte[] serialize(Object source) throws SerializationException {
        if (source == null) {
            return new byte[0];
        }
        
        try {
            String json = objectMapper.writeValueAsString(source);
            log.debug("Serializing object: {} -> JSON length: {}", source.getClass().getSimpleName(), json.length());
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object: {}", source.getClass().getName(), e);
            throw new SerializationException("Failed to serialize object", e);
        }
    }
    
    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        if (source == null || source.length == 0) {
            return null;
        }
        
        try {
            String json = new String(source, StandardCharsets.UTF_8);
            log.debug("Deserializing JSON (length: {}): {}", json.length(), 
                     json.length() > 200 ? json.substring(0, 200) + "..." : json);
            
            Object result = objectMapper.readValue(json, Object.class);
            
            log.debug("Successfully deserialized object: {} (JSON length: {})", 
                     result.getClass().getSimpleName(), json.length());
            return result;
        } catch (Exception e) {
            String json = new String(source, StandardCharsets.UTF_8);
            log.error("Failed to deserialize JSON (length: {}): {}", json.length(), 
                     json.length() > 200 ? json.substring(0, 200) + "..." : json, e);
            throw new SerializationException("Failed to deserialize object", e);
        }
    }
    
}