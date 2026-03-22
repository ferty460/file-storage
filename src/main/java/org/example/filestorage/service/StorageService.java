package org.example.filestorage.service;

import org.example.filestorage.model.dto.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    Resource getResourceInfo(String path, Long userId);

    void deleteResource(String path, Long userId);

    // SomeDownloadResult downloadResource(String path, Long userId);

    Resource moveResource(String from, String to, Long userId);

    Resource searchResource(String path, Long userId);

    List<Resource> uploadResource(String path, List<MultipartFile> files, Long userId);

    Resource createDirectory(String path, Long userId);

    List<Resource> getDirectoryContent(String path, Long userId);
}
