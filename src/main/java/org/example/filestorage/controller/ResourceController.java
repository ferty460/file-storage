package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.User;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.DownloadResult;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.service.StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@Slf4j
@RequiredArgsConstructor
public class ResourceController {

    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<Resource> getResourceInfo(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        Resource resource = storageService.getResourceInfo(path, user.getId());

        log.debug("Retrieved resource info for path: {} by user: {}", path, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resource);
    }

    @GetMapping("/move")
    public ResponseEntity<Resource> moveOrRename(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        Resource resource = storageService.moveResource(from, to, user.getId());

        log.info("Moved resource from: {} to: {} by user: {}", from, to, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Resource>> search(
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        List<Resource> resources = storageService.searchResource(query, user.getId());

        log.debug("Search performed with query: {} by user: {}, found: {} results",
                query, principal.getUsername(), resources.size());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resources);
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        DownloadResult result = storageService.downloadResource(path, user.getId());

        String encodedFileName = URLEncoder.encode(result.resourceName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        log.info("Downloading resource: {} by user: {}", path, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.body());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Resource>> upload(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(required = false, defaultValue = "") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        List<Resource> resources = storageService.uploadResource(path, files, user.getId());

        log.info("Uploaded {} files to path: {} by user: {}", files.size(), path, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resources);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        storageService.deleteResource(path, user.getId());

        log.info("Deleted resource: {} by user: {}", path, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

}
