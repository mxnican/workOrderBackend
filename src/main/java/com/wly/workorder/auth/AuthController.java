package com.wly.workorder.auth;

import com.wly.workorder.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final DefaultAuthService authService;

  public AuthController(DefaultAuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  @GetMapping("/me")
  public ApiResponse<UserProfile> me() {
    return ApiResponse.success(authService.me());
  }

  @GetMapping("/demo-account/{username}")
  public ApiResponse<DemoAccountResponse> demoAccount(@PathVariable String username) {
    return ApiResponse.success(authService.getDemoAccount(username));
  }

  @PutMapping("/profile")
  public ApiResponse<UserProfile> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
    return ApiResponse.success(authService.updateProfile(request));
  }

  @PutMapping("/password")
  public ApiResponse<UserProfile> updatePassword(@RequestBody @Valid UpdatePasswordRequest request) {
    return ApiResponse.success(authService.updatePassword(request));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim();
    }
    if (token != null && !token.isBlank()) {
      authService.logout(token);
    }
    return ApiResponse.success("logged out", null);
  }
}
