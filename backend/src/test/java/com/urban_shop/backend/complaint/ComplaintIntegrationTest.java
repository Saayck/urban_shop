package com.urban_shop.backend.complaint;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.complaint.dto.request.ComplaintCreateRequest;
import com.urban_shop.backend.complaint.dto.request.ComplaintStatusUpdateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import com.urban_shop.backend.complaint.entity.ComplaintType;
import com.urban_shop.backend.complaint.service.ComplaintService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ComplaintIntegrationTest {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void anonymousCustomerCreatesAndAdminProgressesComplaint() {
        Tenant tenant = createTenant("complaint-flow");

        ComplaintResponse created = complaintService.create(
            "complaint-flow",
            null,
            new ComplaintCreateRequest(
                "Juan Perez",
                "DNI",
                "12345678",
                "juan@example.com",
                "987654321",
                ComplaintType.RECLAMO,
                "Recibi un producto defectuoso",
                "Quiero el reembolso"
            )
        );

        assertThat(created.status()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(created.complaintNumber()).startsWith("REC-");
        assertThat(created.customerId()).isNull();
        assertThat(created.customerDocumentType()).isEqualTo("DNI");

        ComplaintResponse inReview = complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.IN_REVIEW)
        );
        assertThat(inReview.status()).isEqualTo(ComplaintStatus.IN_REVIEW);

        complaintService.updateStatus(tenant.getId(), created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.ANSWERED));
        ComplaintResponse closed = complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.CLOSED)
        );
        assertThat(closed.status()).isEqualTo(ComplaintStatus.CLOSED);

        assertThatThrownBy(() -> complaintService.updateStatus(
            tenant.getId(),
            created.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.OPEN)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("cerrado");
    }

    @Test
    void rejectsInvalidDocumentCombination() {
        createTenant("complaint-doc");

        assertThatThrownBy(() -> complaintService.create(
            "complaint-doc",
            null,
            new ComplaintCreateRequest(
                "Maria Lopez",
                "DNI",
                "123",
                null,
                "912345678",
                ComplaintType.QUEJA,
                "Mala atencion",
                null
            )
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("no valido");
    }

    @Test
    void listAdminFiltersCorrectly() {
        Tenant tenant = createTenant("complaint-list");

        complaintService.create("complaint-list", null,
            new ComplaintCreateRequest("Ana", null, null, null, "911111111",
                ComplaintType.RECLAMO, "Primer reclamo", null));
        ComplaintResponse second = complaintService.create("complaint-list", null,
            new ComplaintCreateRequest("Luis", null, null, null, "922222222",
                ComplaintType.QUEJA, "Segunda queja", null));

        complaintService.updateStatus(tenant.getId(), second.id(),
            new ComplaintStatusUpdateRequest(ComplaintStatus.IN_REVIEW));

        assertThat(complaintService.listAdmin(tenant.getId(), null, 0, 20).totalElements())
            .isEqualTo(2);
        assertThat(complaintService.listAdmin(tenant.getId(), ComplaintStatus.OPEN, 0, 20).totalElements())
            .isEqualTo(1);
        assertThat(complaintService.listAdmin(tenant.getId(), ComplaintStatus.IN_REVIEW, 0, 20).totalElements())
            .isEqualTo(1);
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
