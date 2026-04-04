package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseFileStorageException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

}
