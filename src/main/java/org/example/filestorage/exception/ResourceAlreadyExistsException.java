package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends BaseFileStorageException {

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

}
