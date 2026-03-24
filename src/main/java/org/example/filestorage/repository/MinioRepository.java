package org.example.filestorage.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.MinioOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public InputStream getObject(String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (Exception e) {
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
                    throw new MinioOperationException("Error reading object");
                }
            }

            return false;
        } catch (Exception e) {
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
            return 0;
        }
    }

    public List<Item> listObjects(String prefix, boolean recursive) {
        try {
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

            return items;
        } catch (Exception e) {
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
            throw new MinioOperationException("Failed to create directory: " + path);
        }
    }

    public void copyObject(String from, String to) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(to)
                    .source(CopySource.builder()
                            .bucket(bucketName)
                            .object(from)
                            .build())
                    .build());
        } catch (Exception e) {
            throw new MinioOperationException("Failed to copy resource: from " + from + " to " + to);
        }
    }

    public void removeObject(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new MinioOperationException("Failed to delete object with path: " + path);
        }
    }

}
