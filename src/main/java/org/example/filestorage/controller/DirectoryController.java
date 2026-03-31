package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@Slf4j
@RequiredArgsConstructor
public class DirectoryController {

    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<List<Resource>> getDirectoryContent(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Resource> content = storageService.getDirectoryContent(path, principal.user().getId());

        log.debug("Retrieved directory content for path: {} by user: {}", path, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(content);
    }

    @PostMapping
    public ResponseEntity<Resource> create(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Resource directory = storageService.createDirectory(path, principal.user().getId());

        log.info("Created directory: {} by user: {}", path, principal.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(directory);
    }

}
