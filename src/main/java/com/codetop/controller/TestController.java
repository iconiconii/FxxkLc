package com.codetop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple test controller to verify routing is working.
 */
@RestController
@RequestMapping("/api/v1/test")
@Slf4j
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        log.info("Test controller hello endpoint called");
        return ResponseEntity.ok("Hello from test controller!");
    }

    @GetMapping("/codetop-test")
    public ResponseEntity<String> codetopTest() {
        log.info("CodeTop test endpoint called");
        return ResponseEntity.ok("CodeTop routing test successful!");
    }
}