package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseFileStorageException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

}
