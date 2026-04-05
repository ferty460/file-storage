package org.example.filestorage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseFileStorageException extends RuntimeException {

    private final HttpStatus status;

    public BaseFileStorageException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
