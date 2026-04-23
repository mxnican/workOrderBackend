package com.wly.workorder.controller;

import com.wly.workorder.common.ApiResponse;
import com.wly.workorder.model.FileModels.UploadedFile;
import com.wly.workorder.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
  private final FileStorageService fileStorageService;

  public FileController(FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  @PostMapping("/upload")
  public ApiResponse<UploadedFile> upload(@RequestParam("file") MultipartFile file, @RequestParam(required = false) String kind)
    throws IOException {
    return ApiResponse.success("uploaded", fileStorageService.store(file, kind));
  }

  @GetMapping("/{*path}")
  public ResponseEntity<Resource> download(@PathVariable String path) throws IOException {
    String relativePath = normalizePath(path);
    Resource resource = fileStorageService.loadAsResource(relativePath);
    String fileName = Path.of(relativePath).getFileName().toString();
    MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
      .contentType(mediaType)
      .body(resource);
  }

  private String normalizePath(String path) {
    String normalized = Objects.toString(path, "").trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    return normalized;
  }
}
