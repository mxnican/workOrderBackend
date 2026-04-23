package com.wly.workorder.service;

import com.wly.workorder.model.FileModels.UploadedFile;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  UploadedFile store(MultipartFile file, String kind) throws IOException;

  Resource loadAsResource(String relativePath) throws IOException;

  Path resolve(String relativePath);
}
