package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.User;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Resource>> upload(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam(required = false, defaultValue = "") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = principal.user();
        List<Resource> resources = storageService.uploadResource(path, files, user.getId());

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

        return ResponseEntity.noContent().build();
    }

}
