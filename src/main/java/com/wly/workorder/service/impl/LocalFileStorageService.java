package com.wly.workorder.service.impl;

import com.wly.workorder.model.FileModels.UploadedFile;
import com.wly.workorder.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {
  private final Path uploadRoot;

  public LocalFileStorageService(@Value("${workorder.upload-dir:${user.dir}/uploads}") String uploadDir) {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
  }

  @Override
  public UploadedFile store(MultipartFile file, String kind) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("file is empty");
    }

    String originalName = sanitizeFilename(file.getOriginalFilename());
    String ext = extractExtension(originalName);
    String normalizedKind = normalizeKind(kind, ext);
    if ("image".equals(normalizedKind) && !isImageExtension(ext)) {
      throw new IllegalArgumentException("only image files are allowed");
    }
    if ("file".equals(normalizedKind) && !isAttachmentExtension(ext)) {
      throw new IllegalArgumentException("only attachment files are allowed");
    }

    String fileId = UUID.randomUUID().toString().replace("-", "");
    Path relativePath = Paths.get(normalizedKind, String.format(Locale.ROOT, "%04d", LocalDate.now().getYear()),
      String.format(Locale.ROOT, "%02d", LocalDate.now().getMonthValue()),
      String.format(Locale.ROOT, "%02d", LocalDate.now().getDayOfMonth()),
      fileId + (ext.isEmpty() ? "" : "." + ext));
    Path target = uploadRoot.resolve(relativePath).normalize();
    if (!target.startsWith(uploadRoot)) {
      throw new IllegalArgumentException("invalid upload path");
    }

    Files.createDirectories(target.getParent());
    file.transferTo(target);

    String serverPath = relativePath.toString().replace('\\', '/');
    String url = "/api/files/" + serverPath;
    return UploadedFile.builder()
      .fileId(fileId)
      .name(originalName)
      .ext(ext)
      .size(file.getSize())
      .contentType(file.getContentType())
      .serverPath(serverPath)
      .url(url)
      .fileUrl(url)
      .imgUrl(url)
      .build();
  }

  @Override
  public Resource loadAsResource(String relativePath) throws IOException {
    Path target = resolve(relativePath);
    if (!Files.exists(target) || !Files.isRegularFile(target)) {
      throw new IOException("file not found");
    }
    return new PathResource(target);
  }

  @Override
  public Path resolve(String relativePath) {
    Path target = uploadRoot.resolve(relativePath == null ? "" : relativePath).normalize();
    if (!target.startsWith(uploadRoot)) {
      throw new IllegalArgumentException("invalid upload path");
    }
    return target;
  }

  private String normalizeKind(String kind, String ext) {
    String normalized = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    if ("image".equals(normalized)) {
      return "image";
    }
    if ("file".equals(normalized)) {
      return "file";
    }
    return isImageExtension(ext) ? "image" : "file";
  }

  private boolean isImageExtension(String ext) {
    return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "webp".equals(ext) || "bmp".equals(ext);
  }

  private boolean isAttachmentExtension(String ext) {
    return ext.isEmpty() || !isImageExtension(ext);
  }

  private String sanitizeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return "file";
    }
    return filename.replaceAll("[\\\\/]+", "_").trim();
  }

  private String extractExtension(String filename) {
    if (filename == null) {
      return "";
    }
    int index = filename.lastIndexOf('.');
    if (index < 0 || index == filename.length() - 1) {
      return "";
    }
    return filename.substring(index + 1).toLowerCase(Locale.ROOT);
  }
}
