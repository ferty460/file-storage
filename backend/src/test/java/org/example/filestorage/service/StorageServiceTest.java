package org.example.filestorage.service;

import io.minio.*;
import org.example.filestorage.exception.InvalidResourceException;
import org.example.filestorage.exception.ResourceAlreadyExistsException;
import org.example.filestorage.exception.ResourceNotFoundException;
import org.example.filestorage.mapper.ResourceMapper;
import org.example.filestorage.model.dto.response.DownloadResult;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.ResourceType;
import org.example.filestorage.repository.MinioRepository;
import org.example.filestorage.validator.ResourceValidator;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageServiceTest {

    private static final DockerImageName MINIO_IMAGE = DockerImageName.parse(
            "minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1");

    @Container
    protected static final MinIOContainer minioContainer = new MinIOContainer(MINIO_IMAGE)
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    private MinioClient minioClient;
    private MinioRepository minioRepository;
    private ResourcePathService pathService;
    private ResourceValidator validator;
    private ResourceMapper mapper;
    private MinioStorageService storageService;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_PREFIX = "user-1/";
    private static final String BUCKET_NAME = "test-bucket";

    @BeforeAll
    void setUpAll() throws Exception {
        minioClient = MinioClient.builder()
                .endpoint(minioContainer.getS3URL())
                .credentials(minioContainer.getUserName(), minioContainer.getPassword())
                .build();

        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(BUCKET_NAME)
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(BUCKET_NAME)
                    .build());
        }
    }

    @BeforeEach
    void setUp() {
        pathService = new ResourcePathService();
        minioRepository = new MinioRepository(minioClient, BUCKET_NAME);
        validator = new ResourceValidator(minioRepository, pathService);
        mapper = new ResourceMapper(pathService, minioRepository);
        storageService = new MinioStorageService(minioRepository, pathService, validator, mapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        var objects = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(BUCKET_NAME)
                .recursive(true)
                .build());

        for (var result : objects) {
            var item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(item.objectName())
                            .build()
            );
        }
    }

    @Test
    void uploadResource_ShouldUploadSingleFile() {
        String path = "documents/";
        String fileName = "test.txt";
        MockMultipartFile file = createMockFile(fileName, "Hello World");

        List<Resource> resources = storageService.uploadResource(path, List.of(file), TEST_USER_ID);

        assertThat(resources).hasSize(1);
        Resource resource = resources.get(0);
        assertEquals(fileName, resource.getName());
        assertEquals(path, resource.getPath());
        assertEquals(ResourceType.FILE, resource.getType());
        assertEquals(11L, resource.getSize());

        String fullPath = TEST_USER_PREFIX + path + fileName;
        assertTrue(minioRepository.exists(fullPath));
    }

    @Test
    void uploadResource_ShouldUploadMultipleFiles() {
        String path = "uploads/";
        List<MultipartFile> files = List.of(
                createMockFile("file1.txt", "Content 1"),
                createMockFile("file2.txt", "Content 2"),
                createMockFile("file3.txt", "Content 3")
        );

        List<Resource> resources = storageService.uploadResource(path, files, TEST_USER_ID);

        assertThat(resources).hasSize(3);
        assertThat(resources).allMatch(r -> r.getPath().equals("uploads/"));
        assertThat(resources).extracting(Resource::getName)
                .containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");
    }

    @Test
    void uploadResource_ShouldThrowException_WhenFilesListIsEmpty() {
        String path = "empty/";

        assertThatThrownBy(() -> storageService.uploadResource(path, List.of(), TEST_USER_ID))
                .isInstanceOf(InvalidResourceException.class)
                .hasMessageContaining("No files provided for upload");
    }

    @Test
    void uploadResource_ShouldThrowException_WhenFileAlreadyExists() throws Exception {
        String path = "existing/";
        uploadTestFile(TEST_USER_PREFIX + "existing/test.txt", "Existing content");
        MockMultipartFile file = createMockFile("test.txt", "New content");

        assertThatThrownBy(() -> storageService.uploadResource(path, List.of(file), TEST_USER_ID))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("already exist");
    }

    @Test
    void getResourceInfo_ShouldReturnFileInfo() throws Exception {
        String content = "Document content";
        String path = TEST_USER_PREFIX + "docs/readme.txt";
        uploadTestFile(path, content);

        Resource resource = storageService.getResourceInfo("docs/readme.txt", TEST_USER_ID);

        assertNotNull(resource);
        assertEquals("readme.txt", resource.getName());
        assertEquals("docs/", resource.getPath());
        assertEquals(ResourceType.FILE, resource.getType());
        assertEquals(content.length(), resource.getSize());
    }

    @Test
    void getResourceInfo_ShouldReturnDirectoryInfo() throws Exception {
        String path = TEST_USER_PREFIX + "folder/";
        uploadTestFile(path + "file1.txt", "Content");
        uploadTestFile(path + "file2.txt", "Content");

        Resource resource = storageService.getResourceInfo("folder/", TEST_USER_ID);

        assertNotNull(resource);
        assertEquals("folder", resource.getName());
        assertEquals("/", resource.getPath());
        assertEquals(ResourceType.DIRECTORY, resource.getType());
        assertNull(resource.getSize());
    }

    @Test
    void getResourceInfo_ShouldThrowException_WhenResourceNotFound() {
        String file = "hihihaha.txt";
        assertThatThrownBy(() -> storageService.getResourceInfo(file, TEST_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource does not exist: " + file);
    }

    @Test
    void getDirectoryContent_ShouldListDirectoryContents() throws Exception {
        String path = TEST_USER_PREFIX + "project/";
        uploadTestFile(path + "unga/main.java", "Java code");
        uploadTestFile(path + "README.md", "Readme");
        uploadTestFile(path + "bunga/config.yml", "Config");

        List<Resource> contents = storageService.getDirectoryContent("project/", TEST_USER_ID);

        assertThat(contents).hasSize(3);
        assertThat(contents).extracting(Resource::getName)
                .containsExactlyInAnyOrder("unga/", "bunga/", "README.md");
    }

    @Test
    void createDirectory_ShouldCreateNewDirectory() {
        String path = "newfolder/";
        Resource directory = storageService.createDirectory(path, TEST_USER_ID);

        assertNotNull(directory);
        assertEquals("newfolder", directory.getName());
        assertEquals("/", directory.getPath());
        assertEquals(ResourceType.DIRECTORY, directory.getType());

        String fullPath = TEST_USER_PREFIX + "newfolder/";
        assertTrue(minioRepository.exists(fullPath));
    }

    @Test
    void createDirectory_ShouldThrowException_WhenDirectoryAlreadyExists() {
        String path = "folder/";
        storageService.createDirectory(path, TEST_USER_ID);

        assertThatThrownBy(() -> storageService.createDirectory(path, TEST_USER_ID))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Resource already exist: " + path);
    }

    @Test
    void deleteResource_ShouldDeleteFile() throws Exception {
        String file = "eheheh.txt";
        String path = TEST_USER_PREFIX + file;
        uploadTestFile(path, "Content to delete");

        storageService.deleteResource(file, TEST_USER_ID);

        assertFalse(minioRepository.exists(path));
    }

    @Test
    void deleteResource_ShouldDeleteDirectoryRecursively() throws Exception {
        String path = TEST_USER_PREFIX + "recursive/";
        uploadTestFile(path + "file1.txt", "Content 1");
        uploadTestFile(path + "subdir/file2.txt", "Content 2");
        uploadTestFile(path + "subdir/deep/file3.txt", "Content 3");

        storageService.deleteResource("recursive/", TEST_USER_ID);

        assertFalse(minioRepository.exists(path));
        assertFalse(minioRepository.exists(path + "file1.txt"));
        assertFalse(minioRepository.exists(path + "subdir/file2.txt"));
    }

    @Test
    void moveResource_ShouldMoveFile() throws Exception {
        String fromPath = "source/file.txt";
        String toPath = "destination/file.txt";

        uploadTestFile(TEST_USER_PREFIX + "source/file.txt", "File content");

        Resource movedResource = storageService.moveResource(fromPath, toPath, TEST_USER_ID);

        assertEquals("file.txt", movedResource.getName());
        assertEquals("destination/", movedResource.getPath());

        assertFalse(minioRepository.exists(TEST_USER_PREFIX + "source/file.txt"));
        assertTrue(minioRepository.exists(TEST_USER_PREFIX + "destination/file.txt"));
    }

    @Test
    void moveResource_ShouldMoveDirectory() throws Exception {
        String fromPath = "oldfolder/";
        String toPath = "newfolder/";

        uploadTestFile(TEST_USER_PREFIX + "oldfolder/file1.txt", "Content 1");
        uploadTestFile(TEST_USER_PREFIX + "oldfolder/sub/file2.txt", "Content 2");

        Resource movedResource = storageService.moveResource(fromPath, toPath, TEST_USER_ID);

        assertEquals("newfolder", movedResource.getName());
        assertEquals("/", movedResource.getPath());

        assertFalse(minioRepository.exists(TEST_USER_PREFIX + "oldfolder/"));
        assertTrue(minioRepository.exists(TEST_USER_PREFIX + "newfolder/file1.txt"));
        assertTrue(minioRepository.exists(TEST_USER_PREFIX + "newfolder/sub/file2.txt"));
    }

    @Test
    void searchResource_ShouldFindFilesByQuery() throws Exception {
        uploadTestFile(TEST_USER_PREFIX + "docs/report.pdf", "PDF content");
        uploadTestFile(TEST_USER_PREFIX + "docs/presentation.pptx", "PPTX content");
        uploadTestFile(TEST_USER_PREFIX + "images/photo.jpg", "Image content");

        List<Resource> results = storageService.searchResource("docs/", TEST_USER_ID);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Resource::getName)
                .containsExactlyInAnyOrder("report.pdf", "presentation.pptx");
    }

    @Test
    void downloadResource_ShouldReturnFileStream() throws Exception {
        String file = "pomogite.txt";
        uploadTestFile(TEST_USER_PREFIX + file, "Test download content");

        DownloadResult result = storageService.downloadResource(file, TEST_USER_ID);

        assertEquals(file, result.resourceName());
        assertNotNull(result.body());
    }

    @Test
    void downloadResource_ShouldReturnZipStream_ForDirectory() throws Exception {
        String dirPath = TEST_USER_PREFIX + "zipfolder/";
        uploadTestFile(dirPath + "file1.txt", "Content 1");
        uploadTestFile(dirPath + "file2.txt", "Content 2");

        DownloadResult result = storageService.downloadResource("zipfolder/", TEST_USER_ID);

        assertThat(result.resourceName()).endsWith(".zip");
        assertNotNull(result.body());
    }

    @Test
    void shouldHandlePathWithSpacesAndSpecialCharacters() {
        String path = "my documents/";
        MockMultipartFile file = createMockFile("test file.txt", "Content with spaces");

        List<Resource> resources = storageService.uploadResource(path, List.of(file), TEST_USER_ID);

        assertThat(resources).hasSize(1);
        Resource resource = resources.get(0);
        assertEquals("my documents/", resource.getPath());
        assertEquals("test file.txt", resource.getName());
    }

    @Test
    void shouldEnforceUserIsolation() {
        Long user1Id = 1L;
        Long user2Id = 2L;

        storageService.uploadResource("user1/", List.of(createMockFile("file.txt", "User1")), user1Id);
        storageService.uploadResource("user2/", List.of(createMockFile("file.txt", "User2")), user2Id);

        List<Resource> user1Files = storageService.searchResource("user1/", user1Id);
        List<Resource> user2Files = storageService.searchResource("user2/", user2Id);

        assertThat(user1Files).hasSize(1);
        assertThat(user2Files).hasSize(1);

        assertThatThrownBy(() -> storageService.getResourceInfo("user2/file.txt", user1Id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MockMultipartFile createMockFile(String name, String content) {
        return new MockMultipartFile(
                "file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void uploadTestFile(String path, String content) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(path)
                        .stream(new ByteArrayInputStream(content.getBytes()), content.length(), -1)
                        .contentType("text/plain")
                        .build()
        );
    }

}