package org.example.filestorage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for success auth response")
public record SuccessAuthResponse(

        @Schema(description = "Username", examples = "holodec")
        String username

) {
}
