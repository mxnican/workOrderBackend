package com.wly.workorder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class FileModels {
  private FileModels() {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UploadedFile {
    private String fileId;
    private String name;
    private String ext;
    private long size;
    private String contentType;
    private String serverPath;
    private String url;
    private String fileUrl;
    private String imgUrl;
  }
}
