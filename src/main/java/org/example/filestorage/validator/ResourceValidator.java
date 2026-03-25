package org.example.filestorage.validator;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.InvalidResourceException;
import org.example.filestorage.exception.ResourceNotFoundException;
import org.example.filestorage.repository.MinioRepository;
import org.example.filestorage.service.ResourcePathService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ResourceValidator {

    private static final int MAX_PATH_LENGTH = 1024;
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    private static final String ALLOWED_PATH_REGEX = "^[a-zA-Zа-яА-Я0-9\\s/\\-_.]+$";

    private final MinioRepository minioRepository;
    private final ResourcePathService pathService;

    public void fullValidatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidResourceException("Path cannot be null or empty");
        }

        if (!path.matches(ALLOWED_PATH_REGEX)) {
            throw new InvalidResourceException(
                    "Path contains invalid characters. Allowed: letters, numbers, spaces, '-', '/', '_', '.'"
            );
        }

        partialValidatePath(path);
    }

    public void partialValidatePath(String path) {
        if (path.contains("..")) {
            throw new InvalidResourceException("Path cannot contain '..'");
        }

        if (path.length() > MAX_PATH_LENGTH) {
            throw new InvalidResourceException(
                    String.format("Path is too long (max %d characters)", MAX_PATH_LENGTH)
            );
        }
    }

    public void validateNotExistsInBucket(String fullPath, String originalPath) {
        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Resource does not exist: " + originalPath);
        }
    }

    public void validateExistsInBucket(String fullPath, String originalPath) {
        if (minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Resource already exist: " + originalPath);
        }
    }

    public void validateMove(String fullFrom, String fullTo, String userFrom, String userTo) {
        validateNotExistsInBucket(fullFrom, userFrom);
        validateExistsInBucket(fullTo, userTo);

        boolean fromDir = pathService.isDirectoryPath(fullFrom);
        boolean toDir = pathService.isDirectoryPath(fullTo);

        if (fromDir && !toDir) {
            throw new InvalidResourceException("Cannot move directory to file path");
        }
        if (!fromDir && toDir) {
            throw new InvalidResourceException("Cannot move file to directory path");
        }
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidResourceException("File is empty or null");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidResourceException("File name is empty");
        }

        if (fileName.contains("..")) {
            throw new InvalidResourceException("Invalid file name");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidResourceException(
                    String.format("File size exceeds maximum allowed (%d MB)", MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

}
