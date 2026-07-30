package com.urban_shop.backend.email.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Envio de correos transaccionales.
 * <p>
 * Si no hay servidor SMTP configurado ({@code spring.mail.host} vacio) el contenido se
 * escribe en el log en vez de enviarse, para que el entorno de desarrollo funcione sin
 * dependencias externas. En produccion basta con definir la configuracion SMTP.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String SUBJECT_SUFFIX = " - Urban Shop";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:noreply@urbanshop.pe}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        JavaMailSender mailSender = mailEnabled ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null) {
            log.info("[EMAIL SIMULADO] destinatario: {} | asunto: {}", to, subject);
            log.debug("[CUERPO DEL EMAIL]\n{}", htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email enviado a {} | asunto: {}", to, subject);
        } catch (MailException | jakarta.mail.MessagingException ex) {
            // Un fallo de correo no debe tumbar la operacion de negocio que lo dispara.
            log.error("No se pudo enviar el email a {} | asunto: {}", to, subject, ex);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Restablecer contraseña" + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Solicitud de restablecimiento de contraseña</h2>
                <p>Ha solicitado restablecer su contraseña. Utilice el siguiente código/token de recuperación:</p>
                <div style="background: #f4f4f4; padding: 15px; font-weight: bold; font-size: 18px; word-break: break-all;">
                    %s
                </div>
                <p>Este token es válido durante 15 minutos y solo puede usarse una vez.</p>
                <p>Si usted no solicitó este cambio, ignore este mensaje.</p>
            </div>
            """.formatted(escapeHtml(resetToken));

        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendOrderConfirmationEmail(String to, String orderNumber, String totalAmount) {
        String subject = "Confirmación de Pedido #" + orderNumber + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>¡Gracias por tu compra!</h2>
                <p>Tu pedido <strong>#%s</strong> ha sido recibido correctamente.</p>
                <p>Monto Total: <strong>S/ %s</strong></p>
                <p>Te notificaremos tan pronto como tu pedido sea enviado.</p>
            </div>
            """.formatted(escapeHtml(orderNumber), escapeHtml(totalAmount));

        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendOrderStatusEmail(String to, String orderNumber, String statusLabel) {
        String subject = "Actualización de tu pedido #" + orderNumber + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Tu pedido cambió de estado</h2>
                <p>El pedido <strong>#%s</strong> ahora se encuentra en estado <strong>%s</strong>.</p>
                <p>Puedes revisar el detalle completo desde tu cuenta.</p>
            </div>
            """.formatted(escapeHtml(orderNumber), escapeHtml(statusLabel));

        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendComplaintAnsweredEmail(String to, String complaintNumber, String response) {
        String subject = "Respuesta a tu reclamo #" + complaintNumber + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Hemos respondido tu reclamo</h2>
                <p>Reclamo <strong>#%s</strong> del Libro de Reclamaciones.</p>
                <div style="background: #f4f4f4; padding: 15px; white-space: pre-wrap;">%s</div>
                <p>Si la respuesta no resuelve tu caso, puedes acudir a INDECOPI.</p>
            </div>
            """.formatted(escapeHtml(complaintNumber), escapeHtml(response));

        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendComplaintStatusEmail(String to, String complaintNumber, String statusLabel) {
        String subject = "Actualización de tu reclamo #" + complaintNumber + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Tu reclamo cambió de estado</h2>
                <p>El reclamo <strong>#%s</strong> se encuentra ahora en estado <strong>%s</strong>.</p>
            </div>
            """.formatted(escapeHtml(complaintNumber), escapeHtml(statusLabel));

        sendHtmlEmail(to, subject, content);
    }

    @Override
    @Async
    public void sendEmailVerificationEmail(String to, String verificationToken) {
        String subject = "Confirma tu correo" + SUBJECT_SUFFIX;
        String content = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Confirma tu dirección de correo</h2>
                <p>Usa el siguiente código para verificar tu cuenta:</p>
                <div style="background: #f4f4f4; padding: 15px; font-weight: bold; font-size: 18px; word-break: break-all;">
                    %s
                </div>
                <p>El código es válido durante 24 horas.</p>
                <p>Si no creaste esta cuenta, ignora este mensaje.</p>
            </div>
            """.formatted(escapeHtml(verificationToken));

        sendHtmlEmail(to, subject, content);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
