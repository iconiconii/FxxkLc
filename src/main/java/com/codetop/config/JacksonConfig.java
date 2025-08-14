package com.codetop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for Java 8 time module support.
 * 
 * This configuration enables proper serialization/deserialization of Java 8 time types
 * like LocalDateTime, LocalDate, and ZonedDateTime.
 * 
 * @author CodeTop Team
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper with Java 8 time module support.
     * 
     * @return ObjectMapper configured with JSR310 module
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}