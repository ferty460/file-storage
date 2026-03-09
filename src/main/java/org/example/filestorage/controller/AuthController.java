package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.dto.request.LoginRequest;
import org.example.filestorage.model.dto.request.RegisterRequest;
import org.example.filestorage.model.dto.response.SuccessAuthResponse;
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
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessAuthResponse(request.username()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessAuthResponse(request.username()));
    }

}
