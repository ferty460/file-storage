package org.example.filestorage.mapper;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.ResourceType;
import org.example.filestorage.repository.MinioRepository;
import org.example.filestorage.service.ResourcePathService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMapper {

    private final ResourcePathService pathService;
    private final MinioRepository minioRepository;

    public Resource mapItemToResource(Item item, long userId) {
        String objectPath = item.objectName();
        String userPrefix = pathService.getUserRootPath(userId);
        String relativePath = objectPath.substring(userPrefix.length());

        boolean isDirectory = pathService.isDirectoryPath(objectPath);

        if (isDirectory) {
            String folderName = pathService.extractResourceName(relativePath);
            String parentPath = pathService.extractParentPath(relativePath);

            return new Resource(parentPath, folderName, null, ResourceType.DIRECTORY);
        } else {
            String fileName = pathService.extractResourceName(relativePath);
            String parentPath = pathService.extractParentPath(relativePath);
            long size = minioRepository.getFileSize(objectPath);

            return new Resource(parentPath, fileName, size, ResourceType.FILE);
        }
    }

}
