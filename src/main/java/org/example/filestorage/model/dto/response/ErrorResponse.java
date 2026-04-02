package org.example.filestorage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for error response")
public record ErrorResponse(

        @Schema(description = "Error", examples = "Error message")
        String message

) {
}
