package org.example.filestorage.service;

import org.springframework.stereotype.Service;

@Service
public class ResourcePathService {

    private static final String USER_PREFIX_FORMAT = "user-%d/";
    private static final String FULL_PATH_FORMAT = "user-%d/%s";

    public String normalizePathForUser(String path, long userId) {
        String cleanPath = cleanPath(path);
        return FULL_PATH_FORMAT.formatted(userId, cleanPath);
    }

    public String getUserRootPath(long userId) {
        return USER_PREFIX_FORMAT.formatted(userId);
    }

    public String extractResourceName(String path) {
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        String[] components = cleanPath.split("/");

        return components[components.length - 1];
    }

    public String extractParentPath(String path) {
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');

        if (lastSlash == -1) {
            return "/";
        }

        return cleanPath.substring(0, lastSlash + 1);
    }

    public String cleanPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    public boolean isDirectoryPath(String path) {
        return path != null && path.endsWith("/");
    }

    public String ensureTrailingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        return path.endsWith("/") ? path : path + "/";
    }

}
