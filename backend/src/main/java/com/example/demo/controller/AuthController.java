package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // In-memory user storage (for demo purposes)
    // In production, use a proper database
    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final AtomicLong userIdCounter = new AtomicLong(1);
    
    public AuthController() {
        // Empty constructor - initialization happens in @PostConstruct
    }
    
    @PostConstruct
    public void init() {
        // Create default admin user
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", 1L);
        admin.put("username", "admin");
        admin.put("password", passwordEncoder.encode("admin123"));
        admin.put("role", "ROLE_ADMIN");
        users.put("admin", admin);
        
        // Create default user
        Map<String, Object> user = new HashMap<>();
        user.put("id", 2L);
        user.put("username", "user");
        user.put("password", passwordEncoder.encode("user123"));
        user.put("role", "ROLE_USER");
        users.put("user", user);
    }
    
    // Register new user
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }
        
        if (users.containsKey(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }
        
        Long id = userIdCounter.incrementAndGet();
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", id);
        newUser.put("username", username);
        newUser.put("password", passwordEncoder.encode(password));
        newUser.put("role", "ROLE_USER");
        
        users.put(username, newUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("userId", id);
        response.put("username", username);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
        }
        
        Map<String, Object> user = users.get(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }
        
        String storedPassword = (String) user.get("password");
        if (storedPassword == null || !passwordEncoder.matches(password, storedPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }
        
        String token = jwtTokenProvider.generateToken(username);
        String role = (String) user.get("role");
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", username);
        response.put("role", role);
        
        return ResponseEntity.ok(response);
    }
}
