package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidResourceException extends RuntimeException {

    private final HttpStatus status;

    public InvalidResourceException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

}
