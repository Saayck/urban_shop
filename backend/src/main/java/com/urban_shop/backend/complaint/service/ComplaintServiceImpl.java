package com.urban_shop.backend.complaint.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.complaint.dto.request.ComplaintCreateRequest;
import com.urban_shop.backend.complaint.dto.request.ComplaintStatusUpdateRequest;
import com.urban_shop.backend.complaint.dto.response.ComplaintResponse;
import com.urban_shop.backend.complaint.entity.ComplaintBook;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import com.urban_shop.backend.complaint.mapper.ComplaintMapper;
import com.urban_shop.backend.complaint.repository.ComplaintRepository;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository repository;
    private final PublicTenantResolver publicTenantResolver;

    @Override
    @Transactional
    public ComplaintResponse create(String tenantSlug, UUID customerId, ComplaintCreateRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        validateDocument(request);

        ComplaintBook complaint = new ComplaintBook();
        complaint.setTenantId(tenantId);
        complaint.setCustomerId(customerId);
        complaint.setComplaintNumber(generateComplaintNumber());
        complaint.setCustomerFullName(request.fullName().trim());
        complaint.setCustomerDocumentType(trimToNull(request.documentType()));
        complaint.setCustomerDocumentNumber(trimToNull(request.documentNumber()));
        complaint.setCustomerEmail(trimToNull(request.email()));
        complaint.setCustomerPhone(request.phone().trim());
        complaint.setComplaintType(request.complaintType());
        complaint.setDescription(request.description().trim());
        complaint.setRequestedSolution(trimToNull(request.requestedSolution()));
        return ComplaintMapper.toResponse(repository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> listAdmin(
        UUID tenantId,
        ComplaintStatus status,
        int page,
        int size
    ) {
        return PageResponse.from(
            repository.findAdmin(
                tenantId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).map(ComplaintMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getAdmin(UUID tenantId, UUID id) {
        return ComplaintMapper.toResponse(requireComplaint(tenantId, id));
    }

    @Override
    @Transactional
    public ComplaintResponse updateStatus(
        UUID tenantId,
        UUID id,
        ComplaintStatusUpdateRequest request
    ) {
        ComplaintBook complaint = requireComplaint(tenantId, id);
        ComplaintStatus target = request.status();
        if (complaint.getStatus() == target) {
            return ComplaintMapper.toResponse(complaint);
        }
        validateTransition(complaint.getStatus(), target);
        complaint.setStatus(target);
        complaint.setUpdatedAt(LocalDateTime.now());
        return ComplaintMapper.toResponse(repository.save(complaint));
    }

    private void validateTransition(ComplaintStatus current, ComplaintStatus target) {
        if (current == ComplaintStatus.CLOSED) {
            throw new BusinessException("No se puede modificar un reclamo cerrado");
        }
        boolean allowed = switch (current) {
            case OPEN -> target == ComplaintStatus.IN_REVIEW
                || target == ComplaintStatus.ANSWERED
                || target == ComplaintStatus.CLOSED;
            case IN_REVIEW -> target == ComplaintStatus.ANSWERED
                || target == ComplaintStatus.CLOSED;
            case ANSWERED -> target == ComplaintStatus.CLOSED;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(
                "Transicion de estado no permitida: " + current + " -> " + target
            );
        }
    }

    private void validateDocument(ComplaintCreateRequest request) {
        String type = trimToNull(request.documentType());
        String number = trimToNull(request.documentNumber());
        if ((type == null) != (number == null)) {
            throw new BusinessException("Tipo y numero de documento deben enviarse juntos");
        }
        if (type == null) {
            return;
        }
        boolean valid = switch (type) {
            case "DNI" -> number.matches("^\\d{8}$");
            case "CE" -> number.matches("^\\d{9,12}$");
            case "PASSPORT" -> number.matches("^[A-Za-z0-9]{6,12}$");
            case "RUC" -> number.matches("^\\d{11}$");
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("Numero de documento no valido para el tipo " + type);
        }
    }

    private ComplaintBook requireComplaint(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Reclamo no encontrado"));
    }

    private String generateComplaintNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "REC-" + date + "-" + suffix;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
