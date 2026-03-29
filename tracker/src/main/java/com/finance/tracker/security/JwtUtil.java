package com.finance.tracker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 1. The Master Key: Used to cryptographically sign every wristband. 
    // (In a real production app, you hide this in your application.properties file!)
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // 2. Expiration: Wristbands are valid for exactly 24 hours
    private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

    // --- THE PRINTER ---
    // Takes an email and prints a fresh JWT wristband
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    // --- THE SCANNER ---
    // Reads a wristband and extracts the email address printed on it
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // --- THE VALIDATOR ---
    // Checks if the wristband is fake, tampered with, or expired
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // If the signature doesn't match or it's expired, throw them out!
            return false;
        }
    }
}