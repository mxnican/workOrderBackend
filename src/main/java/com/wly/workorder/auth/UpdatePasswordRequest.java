package com.wly.workorder.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
  @NotBlank
  private String oldPassword;

  @NotBlank
  private String newPassword;
}
