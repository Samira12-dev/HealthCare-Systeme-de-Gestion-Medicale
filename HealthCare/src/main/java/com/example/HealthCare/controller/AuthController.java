package com.example.HealthCare.controller;

import com.example.HealthCare.config.JwtUtils;
import com.example.HealthCare.dto.LoginRequestDto;
import com.example.HealthCare.dto.RegisterRequestDto;
import com.example.HealthCare.entity.Patient;
import com.example.HealthCare.entity.Role;
import com.example.HealthCare.entity.User;
import com.example.HealthCare.repository.UserRepo;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto dto) {

        if (dto.getRole() != null && dto.getRole() != Role.PATIENT) {
            return ResponseEntity.badRequest().body("Only patient self-registration is allowed");
        }

        if (userRepo.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        Patient patient = new Patient();
        patient.setUsername(dto.getUsername());
        patient.setEmail(dto.getEmail());
        patient.setPassword(passwordEncoder.encode(dto.getPassword()));
        patient.setRole(Role.PATIENT);
        patient.setNom(dto.getNom());
        patient.setPrenom(dto.getPrenom());
        patient.setTelephone(dto.getTelephone());
        patient.setDateNaissance(dto.getDateNaissance());
        Patient savedUser = userRepo.save(patient);

        String token = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("email", savedUser.getEmail());
        response.put("username", savedUser.getUsername());
        response.put("role", savedUser.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto dto) {

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(),
                            dto.getPassword()
                    )
            );

            if (auth.isAuthenticated()) {

                User user = userRepo.findByEmail(dto.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("type", "Bearer");
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                response.put("username", user.getUsername());
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid login");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid login");
        }
    }


}