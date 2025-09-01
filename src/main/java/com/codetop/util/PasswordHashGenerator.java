package com.codetop.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hashedPassword = encoder.encode(password);
        System.out.println("Plain password: " + password);
        System.out.println("Hashed password: " + hashedPassword);
        System.out.println("Verification: " + encoder.matches(password, hashedPassword));
    }
}