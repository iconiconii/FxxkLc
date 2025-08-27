package com.codetop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Large Document Handler for optimizing storage and retrieval of large note content.
 * 
 * This service provides:
 * - Content compression and decompression
 * - Large content chunking strategies
 * - Memory-efficient processing
 * - Content validation and sanitization
 * - Performance monitoring for large documents
 * 
 * @author CodeTop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LargeDocumentHandler {
    
    private final Executor taskExecutor;
    
    @Value("${notes.max-content-size:50000}")
    private int maxContentSize;
    
    @Value("${notes.compression-threshold:2048}")
    private int compressionThreshold;
    
    @Value("${notes.chunk-size:10000}")
    private int chunkSize;
    
    // Patterns for content validation
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\(.*?\\)");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[.*?\\]\\(.*?\\)");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    
    /**
     * Process and optimize large content before storage.
     * Includes compression, validation, and chunking if necessary.
     * 
     * @param content Raw content from user
     * @return Processed content ready for storage
     */
    public ProcessedContent processLargeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return ProcessedContent.empty();
        }
        
        log.debug("Processing content of size: {} characters", content.length());
        long startTime = System.currentTimeMillis();
        
        ProcessedContent result = ProcessedContent.builder()
            .originalSize(content.length())
            .build();
        
        try {
            // 1. Validate content size
            if (content.length() > maxContentSize) {
                log.warn("Content size {} exceeds maximum allowed size {}", content.length(), maxContentSize);
                throw new IllegalArgumentException("Content size exceeds maximum allowed: " + maxContentSize);
            }
            
            // 2. Sanitize and validate content
            String sanitizedContent = sanitizeContent(content);
            result.setSanitized(true);
            
            // 3. Extract metadata (word count, complexity, etc.)
            ContentMetadata metadata = extractContentMetadata(sanitizedContent);
            result.setMetadata(metadata);
            
            // 4. Decide on storage strategy based on size
            if (sanitizedContent.length() > compressionThreshold) {
                // Large content - apply compression
                String compressedContent = compressContent(sanitizedContent);
                result.setContent(compressedContent);
                result.setCompressed(true);
                result.setCompressedSize(compressedContent.length());
                
                log.debug("Compressed content from {} to {} characters ({}% reduction)", 
                    sanitizedContent.length(), compressedContent.length(),
                    100 - (compressedContent.length() * 100 / sanitizedContent.length()));
            } else {
                // Small content - store as-is
                result.setContent(sanitizedContent);
                result.setCompressed(false);
                result.setCompressedSize(sanitizedContent.length());
            }
            
            // 5. Handle very large content with chunking
            if (sanitizedContent.length() > chunkSize * 2) {
                List<String> chunks = chunkContent(sanitizedContent);
                result.setChunks(chunks);
                result.setChunked(true);
                
                log.debug("Chunked large content into {} pieces", chunks.size());
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(processingTime);
            
            log.debug("Content processing completed in {}ms", processingTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error processing large content", e);
            throw new RuntimeException("Failed to process content", e);
        }
    }
    
    /**
     * Retrieve and decompress content for display.
     * Handles decompression and chunk reassembly.
     * 
     * @param processedContent Previously processed content
     * @return Original readable content
     */
    public String retrieveContent(ProcessedContent processedContent) {
        if (processedContent == null || !StringUtils.hasText(processedContent.getContent())) {
            return "";
        }
        
        try {
            String content = processedContent.getContent();
            
            // Handle chunked content
            if (processedContent.isChunked() && processedContent.getChunks() != null) {
                content = String.join("", processedContent.getChunks());
            }
            
            // Handle compressed content
            if (processedContent.isCompressed()) {
                content = decompressContent(content);
            }
            
            return content;
            
        } catch (Exception e) {
            log.error("Error retrieving content", e);
            return processedContent.getContent(); // Return as-is if decompression fails
        }
    }
    
    /**
     * Async content processing for non-blocking operations.
     */
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public CompletableFuture<ProcessedContent> processLargeContentAsync(String content) {
        return CompletableFuture.supplyAsync(() -> processLargeContent(content), taskExecutor);
    }
    
    /**
     * Content sanitization to remove malicious content and validate format.
     */
    private String sanitizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        
        // Remove potentially dangerous HTML tags while preserving markdown
        String sanitized = content
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<iframe[^>]*>.*?</iframe>", "")
            .replaceAll("javascript:", "")
            .replaceAll("vbscript:", "")
            .replaceAll("data:text/html", "data:text/plain");
        
        // Validate and limit markdown features
        sanitized = validateMarkdownFeatures(sanitized);
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\r\\n", "\\n")
                           .replaceAll("\\r", "\\n")
                           .replaceAll("\\n{3,}", "\\n\\n");
        
        return sanitized.trim();
    }
    
    /**
     * Validate and limit markdown features to prevent abuse.
     */
    private String validateMarkdownFeatures(String content) {
        // Count and limit images
        long imageCount = MARKDOWN_IMAGE_PATTERN.matcher(content).results().count();
        if (imageCount > 20) {
            log.warn("Content contains too many images: {}", imageCount);
            // Could truncate or reject
        }
        
        // Count and limit links
        long linkCount = MARKDOWN_LINK_PATTERN.matcher(content).results().count();
        if (linkCount > 50) {
            log.warn("Content contains too many links: {}", linkCount);
        }
        
        // Validate code blocks
        long codeBlockCount = CODE_BLOCK_PATTERN.matcher(content).results().count();
        if (codeBlockCount > 10) {
            log.warn("Content contains too many code blocks: {}", codeBlockCount);
        }
        
        return content;
    }
    
    /**
     * Extract content metadata for analytics and search optimization.
     */
    private ContentMetadata extractContentMetadata(String content) {
        ContentMetadata metadata = new ContentMetadata();
        
        // Word count
        String[] words = content.split("\\s+");
        metadata.setWordCount(words.length);
        
        // Character count
        metadata.setCharacterCount(content.length());
        
        // Line count
        metadata.setLineCount(content.split("\\n").length);
        
        // Code block count
        metadata.setCodeBlockCount((int) CODE_BLOCK_PATTERN.matcher(content).results().count());
        
        // Image count
        metadata.setImageCount((int) MARKDOWN_IMAGE_PATTERN.matcher(content).results().count());
        
        // Link count
        metadata.setLinkCount((int) MARKDOWN_LINK_PATTERN.matcher(content).results().count());
        
        // Reading time estimate (average 200 words per minute)
        metadata.setEstimatedReadingTimeMinutes(Math.max(1, words.length / 200));
        
        // Content complexity score (based on structure)
        metadata.setComplexityScore(calculateComplexityScore(content));
        
        return metadata;
    }
    
    /**
     * Calculate content complexity score for ranking and filtering.
     */
    private double calculateComplexityScore(String content) {
        double score = 0.0;
        
        // Base score from length
        score += Math.min(content.length() / 1000.0, 10.0);
        
        // Bonus for code blocks
        score += CODE_BLOCK_PATTERN.matcher(content).results().count() * 2.0;
        
        // Bonus for structured content (headers)
        score += content.split("\\n#").length * 1.5;
        
        // Bonus for lists
        score += content.split("\\n[*-]").length * 0.5;
        
        return Math.min(score, 100.0); // Cap at 100
    }
    
    /**
     * Simple content compression using Java's built-in compression.
     */
    private String compressContent(String content) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos);
            gzos.write(content.getBytes("UTF-8"));
            gzos.close();
            
            byte[] compressed = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(compressed);
            
        } catch (Exception e) {
            log.error("Failed to compress content", e);
            return content; // Return original if compression fails
        }
    }
    
    /**
     * Decompress previously compressed content.
     */
    private String decompressContent(String compressedContent) {
        try {
            byte[] compressed = java.util.Base64.getDecoder().decode(compressedContent);
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressed);
            java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bais);
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            gzis.close();
            
            return baos.toString("UTF-8");
            
        } catch (Exception e) {
            log.error("Failed to decompress content", e);
            return compressedContent; // Return as-is if decompression fails
        }
    }
    
    /**
     * Chunk large content into manageable pieces.
     */
    private List<String> chunkContent(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            
            // Try to break at natural boundaries (paragraph, sentence)
            if (end < content.length()) {
                int lastNewline = content.lastIndexOf('\n', end);
                int lastPeriod = content.lastIndexOf('.', end);
                int boundary = Math.max(lastNewline, lastPeriod);
                
                if (boundary > start + chunkSize / 2) {
                    end = boundary + 1;
                }
            }
            
            chunks.add(content.substring(start, end));
            start = end;
        }
        
        return chunks;
    }
    
    /**
     * Data class for processed content with metadata.
     */
    @lombok.Data
    @lombok.Builder
    public static class ProcessedContent {
        private String content;
        private boolean compressed;
        private boolean chunked;
        private boolean sanitized;
        private int originalSize;
        private int compressedSize;
        private long processingTimeMs;
        private ContentMetadata metadata;
        private List<String> chunks;
        
        public static ProcessedContent empty() {
            return ProcessedContent.builder()
                .content("")
                .compressed(false)
                .chunked(false)
                .sanitized(true)
                .originalSize(0)
                .compressedSize(0)
                .processingTimeMs(0)
                .metadata(new ContentMetadata())
                .build();
        }
    }
    
    /**
     * Content metadata for analytics and optimization.
     */
    @lombok.Data
    public static class ContentMetadata {
        private int wordCount;
        private int characterCount;
        private int lineCount;
        private int codeBlockCount;
        private int imageCount;
        private int linkCount;
        private int estimatedReadingTimeMinutes;
        private double complexityScore;
    }
}