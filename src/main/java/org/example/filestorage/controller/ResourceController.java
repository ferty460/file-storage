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
import org.example.filestorage.model.User;
import org.example.filestorage.model.UserPrincipal;
import org.example.filestorage.model.dto.response.DownloadResult;
import org.example.filestorage.model.dto.Resource;
import org.example.filestorage.model.dto.response.ErrorResponse;
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
@Tag(name = "Resource Controller", description = "Controller for managing files and folders in cloud storage")
public class ResourceController {

    private final StorageService storageService;

    @Operation(summary = "Getting information about a resource")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Resource.class),
                            examples = @ExampleObject("""
                    {
                      "path": "documents/",
                      "name": "report.pdf",
                      "size": 1048576,
                      "type": "FILE"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Invalid path provided\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Resource not found: documents/missing.pdf\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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

    @Operation(summary = "Moving or renaming a resource")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Resource.class),
                            examples = @ExampleObject("""
                    {
                      "path": "archive/",
                      "name": "report.pdf",
                      "size": 1048576,
                      "type": "FILE"
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Resource not found: docs/missing.txt\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Resource already exists: archive/report.pdf\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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

    @Operation(summary = "Resource search")
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
                        "size": 1048576,
                        "type": "FILE"
                      },
                      {
                        "path": "archive/",
                        "name": "reports.zip",
                        "size": 5242880,
                        "type": "FILE"
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
                            examples = @ExampleObject("{\"message\":\"Search query cannot be empty\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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

    @Operation(summary = "Downloading a resource")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Resource not found: missing.txt\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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

    @Operation(summary = "Uploading files and folders")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Resource.class)),
                            examples = @ExampleObject("""
                    [
                      {
                        "path": "documents/",
                        "name": "file1.pdf",
                        "size": 1024000,
                        "type": "FILE"
                      },
                      {
                        "path": "documents/",
                        "name": "file2.pdf",
                        "size": 2048000,
                        "type": "FILE"
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
                            examples = @ExampleObject("{\"message\":\"No files provided for upload\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"File already exists: documents/existing.pdf\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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

    @Operation(summary = "Deleting a resource")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject("{\"message\":\"Resource not found: documents/to_delete.txt\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
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
