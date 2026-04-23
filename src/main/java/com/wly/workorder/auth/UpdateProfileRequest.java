package com.wly.workorder.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
  @NotBlank
  private String displayName;

  private String avatarUrl;
}
