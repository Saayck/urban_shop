package com.urban_shop.backend.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.security.WebhookSignatureVerifier;
import com.urban_shop.backend.payment.dto.request.PaymentWebhookRequest;
import com.urban_shop.backend.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirements
@RequestMapping("/api/payments")
@Tag(name = "Payments & Webhooks", description = "Endpoints de pagos y callbacks/webhooks de pasarelas de pago")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/webhook")
    @Operation(
        summary = "Recibir notificacion de pago via Webhook",
        description = """
            Endpoint publico para recibir notificaciones asincronas de pasarelas de pago.
            Requiere las cabeceras X-Webhook-Timestamp y X-Webhook-Signature, donde la firma es
            el HMAC-SHA256 en hexadecimal de "timestamp.cuerpoCrudo" usando el secreto compartido.
            La operacion es idempotente: reenvios del mismo externalTransactionId no duplican pagos.
            """
    )
    public ResponseEntity<Void> handleWebhook(
        @RequestBody String rawBody,
        @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
        @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp
    ) {
        signatureVerifier.verifyPaymentSignature(rawBody, signature, timestamp);

        PaymentWebhookRequest request = parse(rawBody);
        log.info(
            "PAYMENT WEBHOOK: evento verificado del proveedor {} para el pedido {} con estado {}",
            request.provider(),
            request.orderId(),
            request.status()
        );
        paymentService.applyWebhookPayment(request);
        return ResponseEntity.ok().build();
    }

    private PaymentWebhookRequest parse(String rawBody) {
        PaymentWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PaymentWebhookRequest.class);
        } catch (Exception ex) {
            throw new BusinessException("Cuerpo del webhook malformado");
        }

        List<String> violations = validator.validate(request).stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .sorted()
            .toList();
        if (!violations.isEmpty()) {
            throw new BusinessException("Webhook invalido -> " + String.join(", ", violations));
        }
        return request;
    }
}
