package org.example.filestorage.exception;

import org.springframework.http.HttpStatus;

public class MinioOperationException extends BaseFileStorageException {

    public MinioOperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
