package com.finance.tracker.controller;

import com.finance.tracker.dto.AuthRequest;
import com.finance.tracker.dto.AuthResponse;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.security.JwtUtil;
import com.finance.tracker.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // --- DOOR 1: REGISTER ---
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // Hand off the work to the UserService
        userService.registerUser(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    // --- DOOR 2: LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));

            // 1. Generate the token
            String token = jwtUtil.generateToken(authRequest.getEmail());

            // 2. NEW: Look up the actual user to get their ID and Name!
            User user = userRepository.findByEmail(authRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            // 3. Send all three pieces of data back to React
            return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getName()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password!");
        }
    }
}