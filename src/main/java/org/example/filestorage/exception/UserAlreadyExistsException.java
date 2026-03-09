package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserAlreadyExistsException extends RuntimeException {

    private final HttpStatus status;

    public UserAlreadyExistsException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT;
    }

}
