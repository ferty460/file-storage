package org.example.filestorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.response.ErrorResponse;
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
@Tag(name = "User Controller", description = "Controller for getting information about the current user.")
public class UserController {

    @Operation(summary = "Getting information about the current user")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessAuthResponse.class),
                            examples = @ExampleObject("{\"username\":\"john_doe\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"User is not authenticated\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Internal server error...\"}")
                    )
            )
    })
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
