package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.dto.response.UserInfoResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse registerCustomer(RegisterCustomerRequest request);

    UserInfoResponse me(CustomUserDetails principal);
}
