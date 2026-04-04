package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class InvalidResourceException extends BaseFileStorageException {

    public InvalidResourceException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
