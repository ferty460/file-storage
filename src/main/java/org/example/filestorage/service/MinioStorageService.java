package org.example.filestorage.service;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.InvalidResourceException;
import org.example.filestorage.exception.ResourceAlreadyExistsException;
import org.example.filestorage.exception.ResourceNotFoundException;
import org.example.filestorage.mapper.ResourceMapper;
import org.example.filestorage.model.dto.DownloadResult;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.ResourceType;
import org.example.filestorage.repository.MinioRepository;
import org.example.filestorage.validator.ResourceValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioRepository minioRepository;
    private final ResourcePathService pathService;
    private final ResourceValidator validator;
    private final ResourceMapper mapper;

    @Override
    public Resource getResourceInfo(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);
        String resourceName = pathService.extractResourceName(decodedPath);
        String parentPath = pathService.extractParentPath(decodedPath);

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Resource does not exist: " + decodedPath);
        }

        if (pathService.isDirectoryPath(decodedPath)) {
            return new Resource(parentPath, resourceName, null, ResourceType.DIRECTORY);
        }

        long size = minioRepository.getFileSize(fullPath);

        return new Resource(parentPath, resourceName, size, ResourceType.FILE);
    }

    @Override
    public void deleteResource(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Resource does not exist: " + decodedPath);
        }

        List<Item> items = minioRepository.listObjects(fullPath, true);
        if (!items.isEmpty()) {
            for (Item item : items) {
                minioRepository.removeObject(item.objectName());
            }
        }

        minioRepository.removeObject(fullPath);
    }

    @Override
    public DownloadResult downloadResource(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);
        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Resource does not exist: " + decodedPath);
        }

        String resourceName = pathService.extractResourceName(decodedPath);
        if (pathService.isDirectoryPath(decodedPath)) {
            StreamingResponseBody stream = outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    List<Item> items = minioRepository.listObjects(fullPath, true);

                    for (Item item : items) {
                        String objectName = item.objectName();
                        String relativePath = objectName.substring(fullPath.length());

                        if (relativePath.isEmpty()) continue;

                        ZipEntry zipEntry = new ZipEntry(relativePath);
                        zipOut.putNextEntry(zipEntry);

                        try (InputStream is = minioRepository.getObject(objectName)) {
                            is.transferTo(zipOut);
                        }

                        zipOut.closeEntry();
                    }

                    zipOut.finish();
                }
            };

            return new DownloadResult(resourceName + ".zip", stream);
        } else {
            StreamingResponseBody stream = outputStream -> {
                try (InputStream is = minioRepository.getObject(fullPath)) {
                    is.transferTo(outputStream);
                }
            };

            return new DownloadResult(resourceName, stream);
        }
    }

    @Override
    public Resource moveResource(String from, String to, Long userId) {
        String decodedFrom = pathService.decodePath(from);
        String decodedTo = pathService.decodePath(to);
        validator.fullValidatePath(decodedFrom);
        validator.fullValidatePath(decodedTo);

        String fullFrom = pathService.normalizePathForUser(decodedFrom, userId);
        String fullTo = pathService.normalizePathForUser(decodedTo, userId);
        String targetPath = pathService.extractParentPath(to);
        String targetName = pathService.extractResourceName(to);

        validator.validateMove(fullFrom, fullTo, decodedFrom, decodedTo);

        if (pathService.isDirectoryPath(decodedFrom)) {
            List<Item> items = minioRepository.listObjects(fullFrom, true);

            if (!items.isEmpty()) {
                for (Item item : items) {
                    String resource = item.objectName();
                    String relativePath = resource.substring(fullFrom.length());
                    String newPath = fullTo + relativePath;

                    minioRepository.copyObject(resource, newPath);
                    minioRepository.removeObject(resource);
                }
            }

            return new Resource(targetPath, targetName, null, ResourceType.DIRECTORY);
        }

        minioRepository.copyObject(fullFrom, fullTo);
        minioRepository.removeObject(fullFrom);

        return new Resource(targetPath, targetName, minioRepository.getFileSize(fullTo), ResourceType.FILE);
    }

    @Override
    public List<Resource> searchResource(String query, Long userId) {
        String decodedPath = pathService.decodePath(query);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);

        List<Resource> resources = new ArrayList<>();
        for (Item item : minioRepository.listObjects(fullPath, true)) {
            resources.add(mapper.mapItemToResource(item, userId));
        }

        return resources;
    }

    @Override
    public List<Resource> uploadResource(String path, List<MultipartFile> files, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.partialValidatePath(decodedPath);

        if (files == null || files.isEmpty()) {
            throw new InvalidResourceException("No files provided for upload");
        }

        String folderPath = pathService.ensureTrailingSlash(decodedPath);
        String fullFolderPath = pathService.normalizePathForUser(folderPath, userId);

        List<Resource> resources = new ArrayList<>();

        for (MultipartFile file : files) {
            validator.validateFile(file);

            String fileName = Objects.requireNonNull(file.getOriginalFilename()).trim();
            String fullPath = fullFolderPath + fileName;

            if (minioRepository.exists(fullPath)) {
                throw new ResourceAlreadyExistsException(
                        String.format("File already exists: %s%s", folderPath, fileName)
                );
            }

            minioRepository.upload(fullPath, file);

            resources.add(new Resource(folderPath, fileName, file.getSize(), ResourceType.FILE));
        }

        return resources;
    }

    @Override
    public Resource createDirectory(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String pathWithSlash = pathService.ensureTrailingSlash(decodedPath);
        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);
        String directoryName = pathService.extractResourceName(pathWithSlash);
        String parentPath = pathService.extractParentPath(pathWithSlash);

        if (minioRepository.exists(fullPath)) {
            throw new ResourceAlreadyExistsException("Directory already exists: " + decodedPath);
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
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String pathWithSlash = pathService.ensureTrailingSlash(decodedPath);
        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);

        if (!minioRepository.exists(fullPath)) {
            throw new ResourceNotFoundException("Directory does not exist: " + decodedPath);
        }

        List<Item> items = minioRepository.listObjects(fullPath, false);

        return items.stream()
                .filter(item -> !item.objectName().equals(fullPath))
                .map(item -> mapper.mapItemToResource(item, userId))
                .toList();
    }

}
