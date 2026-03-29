package com.finance.tracker.controller;

import com.finance.tracker.dto.AuthRequest;
import com.finance.tracker.dto.AuthResponse;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- DOOR 1: REGISTER ---
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // Trap 1: Did Java actually receive the password from Postman?
        System.out.println("1. Raw Password from Postman: " + user.getPassword());
        
        // Trap 2: Did the encoder successfully scramble it?
        String scrambled = passwordEncoder.encode(user.getPassword());
        System.out.println("2. Scrambled Hash: " + scrambled);
        
        // Save it to the database
        user.setPassword(scrambled);
        userRepository.save(user);
        
        System.out.println("3. Successfully told PostgreSQL to save!");
        
        return ResponseEntity.ok("User registered successfully!");
    }

 // --- DOOR 2: LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

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