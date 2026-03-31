package org.example.filestorage.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.response.SuccessAuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to access /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Current user info retrieved: {}", principal.getUsername());
        return ResponseEntity.ok(new SuccessAuthResponse(principal.getUsername()));
    }

}
