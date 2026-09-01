package com.eventzone.controller;

import com.eventzone.model.User;
import com.eventzone.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.getOrDefault("name", "");
        if (email == null || password == null) return ResponseEntity.badRequest().body(Map.of("error","VALIDATION_ERROR","message","email and password required"));
        User u = authService.register(email,password,name);
        return ResponseEntity.status(201).body(Map.of("id", u.getId(), "email", u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");
        var t = authService.login(email,password);
        if (t.isPresent()) return ResponseEntity.ok(Map.of("token", t.get()));
        return ResponseEntity.status(401).body(Map.of("error","AUTH_FAILED","message","invalid credentials"));
    }
}
