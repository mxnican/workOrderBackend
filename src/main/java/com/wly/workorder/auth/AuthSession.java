package com.wly.workorder.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {
  private String token;
  private String username;
  private String displayName;
  private String avatarUrl;
  private AuthRole role;
}
