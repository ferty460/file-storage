package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseFileStorageException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
