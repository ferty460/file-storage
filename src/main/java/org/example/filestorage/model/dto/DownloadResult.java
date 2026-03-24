package org.example.filestorage.model.dto;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadResult(String resourceName, StreamingResponseBody body) {
}
