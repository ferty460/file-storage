package org.example.filestorage.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO for resource (directory or file)")
public class Resource {

    @Schema(description = "Path", examples = "dir/inner-dir/")
    private String path;

    @Schema(description = "Name", examples = "dir/ or file.zip")
    private String name;

    @Schema(description = "Size", example = "1048576", nullable = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long size;

    @Schema(description = "Type", allowableValues = {"FILE", "DIRECTORY"}, example = "FILE")
    private ResourceType type;

}
