package com.example.auth.service;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.UserCredential;
import com.example.auth.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public String registerUser(RegisterRequest request) {
        UserCredential user = UserCredential.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(request.getRoles())
                .build();
        repository.save(user);
        return "User registered successfully";
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        if (authenticate.isAuthenticated()) {
            UserCredential user = repository.findByUsername(request.getUsername()).orElseThrow();
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles());
            
            String accessToken = jwtService.generateToken(user.getUsername(), claims);
            String refreshToken = jwtService.generateRefreshToken(user.getUsername());
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new RuntimeException("Invalid access");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserCredential user = repository.findByUsername(username).orElseThrow();
        
        if (jwtService.isTokenValid(refreshToken, user.getUsername())) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles());
            
            String accessToken = jwtService.generateToken(user.getUsername(), claims);
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }
}
