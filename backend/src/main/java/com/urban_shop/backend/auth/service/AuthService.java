package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.dto.request.ChangePasswordRequest;
import com.urban_shop.backend.auth.dto.request.ForgotPasswordRequest;
import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RefreshTokenRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.request.ResendVerificationRequest;
import com.urban_shop.backend.auth.dto.request.ResetPasswordRequest;
import com.urban_shop.backend.auth.dto.request.VerifyEmailRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.dto.response.UserInfoResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse registerCustomer(RegisterCustomerRequest request);

    UserInfoResponse me(CustomUserDetails principal);

    /** Renueva el JWT de acceso rotando el refresh token entregado. */
    LoginResponse refresh(RefreshTokenRequest request);

    /** Revoca el refresh token, cerrando la sesion del lado del servidor. */
    void logout(RefreshTokenRequest request);

    void changePassword(CustomUserDetails principal, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    /** Marca el correo como verificado a partir del token enviado tras el registro. */
    void verifyEmail(VerifyEmailRequest request);

    /** Reenvia el correo de verificacion. Responde igual exista o no la cuenta. */
    void resendVerification(ResendVerificationRequest request);
}
