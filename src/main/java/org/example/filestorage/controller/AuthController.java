package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.dto.LoginRequest;
import org.example.filestorage.model.dto.RegisterRequest;
import org.example.filestorage.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request) {
        authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
