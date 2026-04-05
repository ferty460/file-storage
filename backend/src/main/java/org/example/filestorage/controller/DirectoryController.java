package org.example.filestorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.response.ErrorResponse;
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
@Tag(name = "Directory Controller", description = "Controller for working with directory in cloud storage")
public class DirectoryController {

    private final StorageService storageService;

    @Operation(summary = "Getting directory content")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Resource.class)),
                            examples = @ExampleObject("""
                    [
                      {
                        "path": "documents/",
                        "name": "report.pdf",
                        "size": 1024000,
                        "type": "FILE"
                      },
                      {
                        "path": "documents/",
                        "name": "images/",
                        "type": "DIRECTORY"
                      }
                    ]
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Path must end with '/'\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"User is not authenticated\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Directory not found: $dir/\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Internal server error...\"}")
                    )
            )
    })
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

    @Operation(summary = "Creating a new empty folder")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Resource.class),
                            examples = @ExampleObject("""
                    {
                      "path": "documents/",
                      "name": "projects",
                      "type": "DIRECTORY"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Path must end with '/'\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"User is not authenticated\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Parent directory not found: documents/\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Directory already exists: documents/projects/\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Internal server error...\"}")
                    )
            )
    })
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
