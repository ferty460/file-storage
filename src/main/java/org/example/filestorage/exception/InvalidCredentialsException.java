package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidCredentialsException extends RuntimeException {

    private final HttpStatus status;

    public InvalidCredentialsException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
    }

}
