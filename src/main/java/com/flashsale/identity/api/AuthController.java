
package com.flashsale.identity.api;

import com.flashsale.identity.application.AuthService;
import com.flashsale.identity.dto.LoginRequest;
import com.flashsale.identity.dto.LoginResponse;
import com.flashsale.identity.dto.RegisterRequest;
import com.flashsale.identity.dto.UserResponse;
import com.flashsale.dto.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = authService.register(request);

        return ApiResponse.success(
                "User registered successfully", user);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ApiResponse.success(
                "Login successful", response);
    }
}
