package com.urban_shop.backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.common.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticacion, sesion y recuperacion de contrasena")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesion",
        description = "Sin tenantSlug autentica a un usuario interno; con tenantSlug autentica a un cliente de esa tienda."
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register-customer")
    @Operation(summary = "Registrar un cliente en una tienda")
    public ResponseEntity<LoginResponse> registerCustomer(
        @Valid @RequestBody RegisterCustomerRequest request
    ) {
        return ResponseEntity.status(201).body(authService.registerCustomer(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del principal autenticado")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(authService.me(userDetails));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar el token de acceso",
        description = "Rota el refresh token: el entregado queda revocado y se emite uno nuevo."
    )
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Revoca el refresh token para que no pueda emitir nuevos accesos.")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-password")
    @Operation(
        summary = "Cambiar la contrasena propia",
        description = "Requiere la contrasena actual y revoca todas las sesiones abiertas del usuario."
    )
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userDetails, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Solicitar restablecimiento de contrasena",
        description = "Responde 200 exista o no la cuenta, para no filtrar que correos estan registrados."
    )
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer la contrasena con el token recibido por correo")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    @Operation(
        summary = "Verificar la direccion de correo",
        description = "Consume el token enviado tras el registro y marca la cuenta como verificada."
    )
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @Operation(
        summary = "Reenviar el correo de verificacion",
        description = "Responde 200 exista o no la cuenta, para no filtrar que correos estan registrados."
    )
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok().build();
    }
}
