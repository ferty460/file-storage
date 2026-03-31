package org.example.filestorage.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.dto.request.AuthRequest;
import org.example.filestorage.model.dto.response.ErrorResponse;
import org.example.filestorage.model.dto.response.SuccessAuthResponse;
import org.example.filestorage.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        authService.register(request);
        authService.login(request);

        log.info("User registered and logged in: {}", request.username());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new SuccessAuthResponse(request.username()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        authService.login(request);

        log.info("User logged in: {}", request.username());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new SuccessAuthResponse(request.username()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Logout attempted by unauthenticated user");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User is not authenticated"));
        }

        authService.logout();
        clearSessionWithCookie(request, response);

        log.info("User logged out: {}", authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private void clearSessionWithCookie(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}
