package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.dto.response.UserInfoResponse;

import java.util.UUID;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserInfoResponse me(UUID userId);
}
