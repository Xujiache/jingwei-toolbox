package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.AuthData;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.LoginRequest;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.PrincipalData;
import com.xujiache.jingweitoolbox.auth.dto.AuthDtos.RefreshRequest;
import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与会话接口，路径与 docs/api/openapi.json 一致。
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/guest-session")
    public ApiResponse<AuthData> createGuestSession() {
        return ApiResponse.success(authService.createGuestSession());
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthData> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<PrincipalData> me(@AuthenticationPrincipal Jwt principal) {
        return ApiResponse.success(authService.currentUser(principal));
    }
}
