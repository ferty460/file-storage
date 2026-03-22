package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResourceAlreadyExistsException extends RuntimeException {

    private final HttpStatus status;

    public ResourceAlreadyExistsException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT;
    }

}
