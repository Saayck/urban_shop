package com.urban_shop.backend.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Documentacion OpenAPI.
 * <p>
 * El atributo {@code security} del {@link OpenAPIDefinition} es imprescindible: sin el,
 * Swagger UI muestra el boton "Authorize" pero no adjunta el token a las peticiones, de
 * modo que ningun endpoint protegido se puede probar desde la interfaz.
 * Los endpoints publicos se marcan con {@code @SecurityRequirements} (vacio) para quitarles
 * el candado.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Urban Shop Backend API",
        version = "v1",
        description = """
            API multi-tenant para tiendas de ropa urbana.

            Superficies:
            - `/api/store/{slug}/**` publico, sin autenticacion (catalogo de la tienda)
            - `/api/auth/**` autenticacion y recuperacion de contrasena
            - `/api/customer/**` requiere JWT con rol CUSTOMER
            - `/api/admin/**` requiere JWT con rol TENANT_ADMIN o SALES_STAFF
            - `/api/super-admin/**` requiere JWT con rol SUPER_ADMIN

            Para probar endpoints protegidos: haz POST a `/api/auth/login`, copia el campo
            `token` de la respuesta y pegalo en el boton Authorize (sin el prefijo "Bearer").
            """
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Token JWT obtenido en /api/auth/login"
)
public class OpenApiConfig {
}
