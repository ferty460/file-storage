package org.example.filestorage.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.example.filestorage.exception.MinioOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public boolean exists(String fullPath) {
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
            throw new MinioOperationException("Failed to check resource existence");
        } catch (Exception e) {
            throw new MinioOperationException("Failed to check resource existence");
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

}
