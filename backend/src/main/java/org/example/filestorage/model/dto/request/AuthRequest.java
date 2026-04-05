package org.example.filestorage.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO for user's authentication")
public record AuthRequest(

        @Schema(description = "Username", examples = "holodec")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must contain from 3 to 64 characters.")
        String username,

        @Schema(description = "Password", examples = "holo123dec#")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 32, message = "Password must contain from 6 to 32 characters.")
        String password

) {
}
