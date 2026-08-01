package com.urban_shop.backend.complaint;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.complaint.dto.request.ComplaintCreateRequest;
import com.urban_shop.backend.complaint.dto.request.ComplaintStatusUpdateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import com.urban_shop.backend.complaint.entity.ComplaintType;
import com.urban_shop.backend.complaint.service.ComplaintService;
import com.urban_shop.backend.email.service.EmailService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class ComplaintResponseIntegrationTest {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private TenantRepository tenantRepository;

    @MockBean
    private EmailService emailService;

    @Test
    void requiresAWrittenResponseToAnswerAComplaint() {
        String slug = "complaint-answer";
        Tenant tenant = createTenant(slug);
        ComplaintResponse created = createComplaint(slug, "consumidor@urban.pe");

        // El Libro de Reclamaciones exige constancia escrita de la respuesta.
        assertThatThrownBy(() -> complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.ANSWERED, null)
        )).isInstanceOf(BusinessException.class).hasMessageContaining("respuesta al consumidor");

        ComplaintResponse answered = complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.ANSWERED, "Se procedio al cambio del producto")
        );

        assertThat(answered.status()).isEqualTo(ComplaintStatus.ANSWERED);
        assertThat(answered.response()).isEqualTo("Se procedio al cambio del producto");
        assertThat(answered.respondedAt()).isNotNull();
    }

    @Test
    void notifiesTheConsumerWhenTheComplaintIsAnswered() {
        String slug = "complaint-notify";
        Tenant tenant = createTenant(slug);
        ComplaintResponse created = createComplaint(slug, "consumidor@urban.pe");

        complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.IN_REVIEW, null)
        );
        verify(emailService).sendComplaintStatusEmail(
            eq("consumidor@urban.pe"),
            eq(created.complaintNumber()),
            eq("IN_REVIEW")
        );

        complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.ANSWERED, "Reembolso emitido")
        );
        verify(emailService).sendComplaintAnsweredEmail(
            eq("consumidor@urban.pe"),
            eq(created.complaintNumber()),
            eq("Reembolso emitido")
        );
    }

    @Test
    void skipsTheNotificationWhenTheComplaintHasNoEmail() {
        String slug = "complaint-no-email";
        Tenant tenant = createTenant(slug);
        ComplaintResponse created = createComplaint(slug, null);

        complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.ANSWERED, "Atendido por telefono")
        );

        verify(emailService, never()).sendComplaintAnsweredEmail(anyString(), anyString(), anyString());
        verify(emailService, never()).sendComplaintStatusEmail(anyString(), anyString(), anyString());
    }

    private ComplaintResponse createComplaint(String slug, String email) {
        return complaintService.create(slug, null, new ComplaintCreateRequest(
            "Ana Torres",
            "DNI",
            "12345678",
            email,
            "987654321",
            ComplaintType.RECLAMO,
            "El producto llego danado",
            "Cambio del producto"
        ));
    }

    private Tenant createTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug);
        tenant.setStatus("ACTIVE");
        tenant.setPlanName("BASIC");
        return tenantRepository.save(tenant);
    }
}
