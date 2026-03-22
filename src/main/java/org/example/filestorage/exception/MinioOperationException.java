package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MinioOperationException extends RuntimeException {

    private final HttpStatus status;

    public MinioOperationException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
