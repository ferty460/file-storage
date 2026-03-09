package org.example.filestorage.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must contain from 3 to 64 characters.")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 32, message = "Password must contain from 6 to 32 characters.")
        String password

) {
}
