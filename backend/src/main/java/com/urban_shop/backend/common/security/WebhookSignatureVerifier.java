package com.urban_shop.backend.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Valida la firma HMAC-SHA256 que las pasarelas de pago envian junto al webhook.
 * <p>
 * La firma se calcula sobre {@code timestamp + "." + cuerpoCrudo}, de modo que un
 * atacante no puede reenviar una notificacion capturada fuera de la ventana de tolerancia.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final WebhookProperties webhookProperties;

    public void verifyPaymentSignature(String rawBody, String signatureHeader, String timestampHeader) {
        String secret = webhookProperties.getPayment().getSecret();
        if (secret == null || secret.isBlank()) {
            log.error("PAYMENT WEBHOOK: rechazado porque app.webhook.payment.secret no esta configurado");
            throw new AccessDeniedException("Webhook de pagos no habilitado");
        }
        if (signatureHeader == null || signatureHeader.isBlank() || timestampHeader == null || timestampHeader.isBlank()) {
            throw new AccessDeniedException("Firma de webhook ausente");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Timestamp de webhook invalido");
        }

        long ageSeconds = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (ageSeconds > webhookProperties.getPayment().getToleranceSeconds()) {
            log.warn("PAYMENT WEBHOOK: firma fuera de la ventana de tolerancia ({}s de antiguedad)", ageSeconds);
            throw new AccessDeniedException("Firma de webhook expirada");
        }

        String expected = hmacHex(secret, timestamp + "." + rawBody);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            log.warn("SECURITY AUDIT: firma de webhook de pagos invalida");
            throw new AccessDeniedException("Firma de webhook invalida");
        }
    }

    private String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC del webhook", ex);
        }
    }
}
