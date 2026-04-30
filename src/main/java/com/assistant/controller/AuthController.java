package com.assistant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.password:admin123}")
    private String appPassword;

    private String currentToken = null;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (appPassword.equals(password)) {
            currentToken = UUID.randomUUID().toString();
            return ResponseEntity.ok(Map.of("token", currentToken));
        }
        return ResponseEntity.status(401).body(Map.of("error", "密码错误"));
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam String token) {
        if (currentToken != null && currentToken.equals(token)) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false));
    }
}