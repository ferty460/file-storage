package org.example.filestorage.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.exception.MinioOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class MinioRepository {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioRepository(MinioClient minioClient, @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public InputStream getObject(String path) {
        try {
            log.debug("Getting object: {}", path);
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get object by path: {}", path, e);
            throw new MinioOperationException("Failed to get object by path: " + path);
        }
    }

    public boolean exists(String fullPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());

            return true;
        } catch (ErrorResponseException e) {
            if (!e.errorResponse().code().equals("NoSuchKey")) {
                log.error("Error checking existence for path: {}", fullPath, e);
                throw new MinioOperationException("Failed to check resource existence");
            }

            String dirPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(dirPath)
                    .recursive(false)
                    .maxKeys(1)
                    .build());

            for (Result<Item> result : results) {
                try {
                    String objectName = result.get().objectName();
                    if (objectName.startsWith(dirPath)) {
                        return true;
                    }
                } catch (Exception ex) {
                    log.error("Error reading object", ex);
                    throw new MinioOperationException("Error reading object");
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to check resource existence for path: {}", fullPath, e);
            throw new MinioOperationException("Failed to check resource existence");
        }
    }

    public long getFileSize(String fullPath) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());

            return stat.size();
        } catch (Exception e) {
            log.warn("Failed to get file size for path: {}", fullPath, e);
            return 0;
        }
    }

    public List<Item> listObjects(String prefix, boolean recursive) {
        try {
            log.debug("Listing objects with prefix: {} (recursive: {})", prefix, recursive);
            List<Item> items = new ArrayList<>();

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(recursive)
                            .build()
            );

            for (Result<Item> result : results) {
                items.add(result.get());
            }

            log.debug("Found {} objects with prefix: {}", items.size(), prefix);
            return items;
        } catch (Exception e) {
            log.error("Failed to list objects with prefix: {}", prefix, e);
            throw new MinioOperationException("Failed to list objects with prefix: " + prefix);
        }
    }

    public void upload(String path, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload file: {}", path, e);
            throw new MinioOperationException("Failed to upload file: " + path);
        }
    }

    public void createDirectory(String path) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create directory: {}", path, e);
            throw new MinioOperationException("Failed to create directory: " + path);
        }
    }

    public void copyObject(String from, String to) {
        try {
            log.info("Copying object from: {} to: {}", from, to);
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(to)
                    .source(CopySource.builder()
                            .bucket(bucketName)
                            .object(from)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error("Failed to copy resource: from {} to {}", from, to, e);
            throw new MinioOperationException("Failed to copy resource: from " + from + " to " + to);
        }
    }

    public void removeObject(String path) {
        try {
            log.info("Removing object: {}", path);
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete object with path: {}", path, e);
            throw new MinioOperationException("Failed to delete object with path: " + path);
        }
    }

}
