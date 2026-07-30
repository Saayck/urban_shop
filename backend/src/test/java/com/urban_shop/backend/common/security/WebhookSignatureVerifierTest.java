package com.urban_shop.backend.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "secreto-compartido-de-pruebas";
    private static final String BODY = "{\"orderId\":\"3f2504e0-4f89-11d3-9a0c-0305e82c3301\",\"status\":\"COMPLETED\"}";

    private WebhookProperties properties;
    private WebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = new WebhookProperties();
        properties.getPayment().setSecret(SECRET);
        verifier = new WebhookSignatureVerifier(properties);
    }

    @Test
    void acceptsASignatureCalculatedWithTheSharedSecret() {
        long timestamp = Instant.now().getEpochSecond();

        assertThatCode(() -> verifier.verifyPaymentSignature(
            BODY,
            sign(SECRET, timestamp + "." + BODY),
            String.valueOf(timestamp)
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsASignatureMadeWithAnotherSecret() {
        long timestamp = Instant.now().getEpochSecond();

        assertThatThrownBy(() -> verifier.verifyPaymentSignature(
            BODY,
            sign("secreto-del-atacante", timestamp + "." + BODY),
            String.valueOf(timestamp)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsATamperedBodyEvenWithAValidSignatureFormat() {
        long timestamp = Instant.now().getEpochSecond();
        String signature = sign(SECRET, timestamp + "." + BODY);

        assertThatThrownBy(() -> verifier.verifyPaymentSignature(
            BODY.replace("COMPLETED", "REFUNDED"),
            signature,
            String.valueOf(timestamp)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAReplayOutsideTheToleranceWindow() {
        long staleTimestamp = Instant.now().getEpochSecond() - 3600;

        assertThatThrownBy(() -> verifier.verifyPaymentSignature(
            BODY,
            sign(SECRET, staleTimestamp + "." + BODY),
            String.valueOf(staleTimestamp)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsEverythingWhenNoSecretIsConfigured() {
        properties.getPayment().setSecret("");
        long timestamp = Instant.now().getEpochSecond();

        assertThatThrownBy(() -> verifier.verifyPaymentSignature(
            BODY,
            sign(SECRET, timestamp + "." + BODY),
            String.valueOf(timestamp)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsRequestsWithoutSignatureHeaders() {
        assertThatThrownBy(() -> verifier.verifyPaymentSignature(BODY, null, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    private String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
