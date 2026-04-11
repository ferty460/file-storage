package org.example.filestorage.service;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.exception.InvalidResourceException;
import org.example.filestorage.mapper.ResourceMapper;
import org.example.filestorage.model.dto.response.DownloadResult;
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
@Slf4j
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

        validator.validateNotExistsInBucket(fullPath, decodedPath);

        if (pathService.isDirectoryPath(decodedPath)) {
            return new Resource(parentPath, resourceName, null, ResourceType.DIRECTORY);
        }

        long size = minioRepository.getFileSize(fullPath);

        log.debug("Getting resource info for path: {} (user: {})", decodedPath, userId);
        return new Resource(parentPath, resourceName, size, ResourceType.FILE);
    }

    @Override
    public void deleteResource(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);
        validator.validateNotExistsInBucket(fullPath, decodedPath);

        deleteRecursively(fullPath);
        log.info("Deleting resource: {} (user: {})", decodedPath, userId);
    }

    @Override
    public DownloadResult downloadResource(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);
        validator.validateNotExistsInBucket(fullPath, decodedPath);

        String resourceName = pathService.extractResourceName(decodedPath);
        if (pathService.isDirectoryPath(decodedPath)) {
            return new DownloadResult(resourceName + ".zip", buildZipStream(fullPath));
        }

        log.info("Downloading resource: {} (user: {})", decodedPath, userId);
        return new DownloadResult(resourceName, buildFileStream(fullPath));
    }

    @Override
    public Resource moveResource(String from, String to, Long userId) {
        String decodedFrom = pathService.decodePath(from);
        String decodedTo = pathService.decodePath(to);
        validator.fullValidatePath(decodedFrom);
        validator.fullValidatePath(decodedTo);

        String decodedFromWithSlash = pathService.ensureTrailingSlash(decodedFrom);
        String decodedToWithSlash = pathService.ensureTrailingSlash(decodedTo);
        boolean isFolder = minioRepository.exists(pathService.normalizePathForUser(decodedFromWithSlash, userId));

        String fromPath = isFolder ? decodedFromWithSlash : decodedFrom;
        String toPath = isFolder ? decodedToWithSlash : decodedTo;
        String fullFrom = pathService.normalizePathForUser(fromPath, userId);
        String fullTo = pathService.normalizePathForUser(toPath, userId);
        String targetPath = pathService.extractParentPath(decodedTo);
        String targetName = pathService.extractResourceName(decodedTo);

        validator.validateMove(fullFrom, fullTo, decodedFrom, decodedTo);

        if (isFolder) {
            moveDirectory(fullFrom, fullTo);
            minioRepository.removeObject(fullFrom);

            log.info("Moving directory from: {} to: {} (user: {})", decodedFrom, decodedTo, userId);
            return new Resource(targetPath, targetName, null, ResourceType.DIRECTORY);
        }

        minioRepository.copyObject(fullFrom, fullTo);
        minioRepository.removeObject(fullFrom);

        log.info("Moving file from: {} to: {} (user: {})", decodedFrom, decodedTo, userId);
        return new Resource(targetPath, targetName, minioRepository.getFileSize(fullTo), ResourceType.FILE);
    }

    @Override
    public List<Resource> searchResource(String query, Long userId) {
        String decodedPath = pathService.decodePath(query);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(decodedPath, userId);

        log.debug("Searching resources with query: {} (user: {})", decodedPath, userId);

        List<Resource> resources = new ArrayList<>();
        for (Item item : minioRepository.listObjects(fullPath, true)) {
            resources.add(mapper.mapItemToResource(item, userId));
        }

        log.debug("Search found {} results", resources.size());
        return resources;
    }

    @Override
    public List<Resource> uploadResource(String path, List<MultipartFile> files, Long userId) {
        String decodedPath = pathService.decodePath(path);
        String folderPath = pathService.ensureTrailingSlash(decodedPath);
        validator.partialValidatePath(folderPath);

        if (files == null || files.isEmpty()) {
            log.warn("Upload attempted with no files by user: {}", userId);
            throw new InvalidResourceException("No files provided for upload");
        }

        String fullFolderPath = pathService.normalizePathForUser(folderPath, userId);

        log.info("Uploading {} files to path: {} (user: {})", files.size(), folderPath, userId);
        List<Resource> resources = new ArrayList<>();

        for (MultipartFile file : files) {
            validator.validateFile(file);

            String fileName = Objects.requireNonNull(file.getOriginalFilename()).trim();
            String fullPath = fullFolderPath + fileName;

            validator.validateExistsInBucket(fullPath, folderPath + fileName);

            minioRepository.upload(fullPath, file);

            log.debug("Uploaded file: {}", fileName);
            resources.add(new Resource(folderPath, fileName, file.getSize(), ResourceType.FILE));
        }

        return resources;
    }

    @Override
    public Resource createDirectory(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        String pathWithSlash = pathService.ensureTrailingSlash(decodedPath);
        validator.fullValidatePath(decodedPath);

        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);
        String directoryName = pathService.extractResourceName(pathWithSlash);
        String parentPath = pathService.extractParentPath(pathWithSlash);

        validator.validateExistsInBucket(fullPath, pathWithSlash);

        if (!parentPath.isEmpty() && !parentPath.equals("/")) {
            String fullParentPath = pathService.normalizePathForUser(parentPath, userId);

            validator.validateNotExistsInBucket(fullParentPath, parentPath);
        }

        log.info("Creating directory: {} (user: {})", pathWithSlash, userId);
        minioRepository.createDirectory(fullPath);

        return new Resource(parentPath, directoryName, null, ResourceType.DIRECTORY);
    }

    @Override
    public List<Resource> getDirectoryContent(String path, Long userId) {
        String decodedPath = pathService.decodePath(path);
        String pathWithSlash = pathService.ensureTrailingSlash(decodedPath);
        validator.fullValidatePath(pathWithSlash);

        String fullPath = pathService.normalizePathForUser(pathWithSlash, userId);

        validator.validateNotExistsInBucket(fullPath, pathWithSlash);

        log.debug("Getting directory content for path: {} (user: {})", pathWithSlash, userId);
        List<Item> items = minioRepository.listObjects(fullPath, false);

        return items.stream()
                .filter(item -> !item.objectName().equals(fullPath))
                .map(item -> mapper.mapItemToResource(item, userId))
                .toList();
    }

    private void deleteRecursively(String fullPath) {
        List<Item> items = minioRepository.listObjects(fullPath, true);

        log.debug("Deleting {} objects recursively from: {}", items.size(), fullPath);
        for (Item item : items) {
            minioRepository.removeObject(item.objectName());
        }
    }

    private StreamingResponseBody buildFileStream(String fullPath) {
        return outputStream -> {
            try (InputStream is = minioRepository.getObject(fullPath)) {
                is.transferTo(outputStream);
            }
        };
    }

    private StreamingResponseBody buildZipStream(String fullPath) {
        return outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                List<Item> items = minioRepository.listObjects(fullPath, true);

                log.debug("Zipping {} items from directory: {}", items.size(), fullPath);
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
    }

    private void moveDirectory(String from, String to) {
        List<Item> items = minioRepository.listObjects(from, true);

        log.debug("Moving directory from: {} to: {}, containing {} items", from, to, items.size());

        for (Item item : items) {
            String objectName = item.objectName();

            if (objectName.equals(from)) {
                continue;
            }

            String relativePath = objectName.substring(from.length());
            String newPath = to + relativePath;

            minioRepository.copyObject(objectName, newPath);
            minioRepository.removeObject(objectName);
        }

        minioRepository.removeObject(from);

        log.debug("Directory moved successfully, old directory removed: {}", from);
    }

}
