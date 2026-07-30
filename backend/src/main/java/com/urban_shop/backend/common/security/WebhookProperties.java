package com.urban_shop.backend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook")
@Getter
@Setter
public class WebhookProperties {

    private final Payment payment = new Payment();

    @Getter
    @Setter
    public static class Payment {

        /**
         * Secreto compartido con la pasarela para firmar el cuerpo del webhook.
         * Sin secreto configurado el endpoint rechaza todas las notificaciones.
         */
        private String secret;

        /**
         * Ventana maxima de antiguedad de la firma, para evitar reenvios (replay).
         */
        private long toleranceSeconds = 300;
    }
}
