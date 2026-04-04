package org.example.filestorage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Schema(description = "DTO for download resource")
public record DownloadResult(

        @Schema(description = "Resource name", examples = "file.zip")
        String resourceName,

        @Schema(description = "A stream with resource data")
        StreamingResponseBody body

) {
}
