package org.example.filestorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<Resource> create(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Resource directory = storageService.createDirectory(path, principal.user().getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(directory);
    }

}
