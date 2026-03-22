package org.example.filestorage.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.InvalidResourceException;
import org.example.filestorage.exception.ResourceAlreadyExistsException;
import org.example.filestorage.exception.ResourceNotFoundException;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final ResourcePathService pathService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public Resource getResourceInfo(String path, Long userId) {
        return null;
    }

    @Override
    public void deleteResource(String path, Long userId) {

    }

    @Override
    public Resource moveResource(String from, String to, Long userId) {
        return null;
    }

    @Override
    public Resource searchResource(String path, Long userId) {
        return null;
    }

    @Override
    public List<Resource> uploadResource(String path, List<MultipartFile> files, Long userId) {
        return Collections.emptyList();
    }

    @Override
    public Resource createDirectory(String path, Long userId) {
        validateInputParameters(path);

        String pathWithSlash = pathService.ensureTrailingSlash(path);
        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);
        String directoryName = pathService.extractResourceName(pathWithSlash);
        String parentPath = pathService.extractParentPath(pathWithSlash);

        validateBusinessRules(path, userId, fullPath, parentPath);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + fullPath, e);
        }

        return new Resource(parentPath, directoryName, null, ResourceType.DIRECTORY);
    }

    private void validateInputParameters(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidResourceException("Path cannot be null or empty");
        }
        if (!path.matches("^[a-zA-Zа-яА-Я0-9\\s/\\-_.]+$")) {
            throw new InvalidResourceException(
                    "Path contains invalid characters. Allowed: letters, numbers, spaces, '-', '_', '.'"
            );
        }
        if (path.contains("..")) {
            throw new InvalidResourceException("Path cannot contain '..'");
        }
        if (path.length() > 1024) {
            throw new InvalidResourceException(
                    String.format("Path is too long (max %d characters)", 1024)
            );
        }
    }

    private void validateBusinessRules(String path, Long userId, String fullPath, String parentPath) {
        if (resourceExists(fullPath)) {
            throw new ResourceAlreadyExistsException("Directory already exists: " + path);
        }
        if (!parentPath.isEmpty() && !parentPath.equals("/")) {
            String fullParentPath = pathService.normalizePathForUser(parentPath, userId);
            if (!resourceExists(fullParentPath)) {
                throw new ResourceNotFoundException("Parent directory does not exist: " + parentPath);
            }
        }
    }

    private boolean resourceExists(String fullPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("Failed to check resource existence", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check resource existence", e);
        }
    }

}
