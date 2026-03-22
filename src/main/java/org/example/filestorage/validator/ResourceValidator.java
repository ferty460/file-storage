package org.example.filestorage.validator;

import org.example.filestorage.exception.InvalidResourceException;
import org.springframework.stereotype.Component;

@Component
public class ResourceValidator {

    private static final int MAX_PATH_LENGTH = 1024;
    private static final String ALLOWED_PATH_REGEX = "^[a-zA-Zа-яА-Я0-9\\s/\\-_.]+$";

    public void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidResourceException("Path cannot be null or empty");
        }

        if (!path.matches(ALLOWED_PATH_REGEX)) {
            throw new InvalidResourceException(
                    "Path contains invalid characters. Allowed: letters, numbers, spaces, '-', '/', '_', '.'"
            );
        }

        if (path.contains("..")) {
            throw new InvalidResourceException("Path cannot contain '..'");
        }

        if (path.length() > MAX_PATH_LENGTH) {
            throw new InvalidResourceException(
                    String.format("Path is too long (max %d characters)", MAX_PATH_LENGTH)
            );
        }
    }

}
