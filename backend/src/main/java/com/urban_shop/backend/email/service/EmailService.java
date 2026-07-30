package com.urban_shop.backend.email.service;

public interface EmailService {

    void sendHtmlEmail(String to, String subject, String htmlContent);

    void sendPasswordResetEmail(String to, String resetToken);

    void sendOrderConfirmationEmail(String to, String orderNumber, String totalAmount);

    void sendOrderStatusEmail(String to, String orderNumber, String statusLabel);

    /** Comunica al consumidor la respuesta formal a su reclamo. */
    void sendComplaintAnsweredEmail(String to, String complaintNumber, String response);

    void sendComplaintStatusEmail(String to, String complaintNumber, String statusLabel);

    /** Envia el enlace/codigo para confirmar la direccion de correo tras el registro. */
    void sendEmailVerificationEmail(String to, String verificationToken);
}
