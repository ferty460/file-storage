package org.example.filestorage.service;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.ResourceAlreadyExistsException;
import org.example.filestorage.exception.ResourceNotFoundException;
import org.example.filestorage.mapper.ResourceMapper;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.ResourceType;
import org.example.filestorage.repository.MinioRepository;
import org.example.filestorage.validator.ResourceValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioRepository minioRepository;
    private final ResourcePathService pathService;
    private final ResourceValidator validator;
    private final ResourceMapper mapper;

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
        validator.validatePath(path);

        String pathWithSlash = pathService.ensureTrailingSlash(path);
        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);
        String directoryName = pathService.extractResourceName(pathWithSlash);
        String parentPath = pathService.extractParentPath(pathWithSlash);

        if (minioRepository.exists(fullPath)) {
            throw new ResourceAlreadyExistsException("Directory already exists: " + path);
        }
        if (!parentPath.isEmpty() && !parentPath.equals("/")) {
            String fullParentPath = pathService.normalizePathForUser(parentPath, userId);
            if (!minioRepository.exists(fullParentPath)) {
                throw new ResourceNotFoundException("Parent directory does not exist: " + parentPath);
            }
        }

        minioRepository.createDirectory(fullPath);

        return new Resource(parentPath, directoryName, null, ResourceType.DIRECTORY);
    }

    @Override
    public List<Resource> getDirectoryContent(String path, Long userId) {
        validator.validatePath(path);

        String pathWithSlash = pathService.ensureTrailingSlash(path);
        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Directory does not exist: " + path);
        }

        List<Item> items = minioRepository.listObjects(fullPath, false);

        return items.stream()
                .filter(item -> !item.objectName().equals(fullPath))
                .map(item -> mapper.mapItemToResource(item, userId))
                .toList();
    }

}
