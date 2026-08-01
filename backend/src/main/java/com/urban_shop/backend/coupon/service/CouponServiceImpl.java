package com.urban_shop.backend.coupon.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.coupon.dto.request.CouponCreateRequest;
import com.urban_shop.backend.coupon.dto.request.CouponUpdateRequest;
import com.urban_shop.backend.coupon.dto.response.CouponPreviewResponse;
import com.urban_shop.backend.coupon.dto.response.CouponResponse;
import com.urban_shop.backend.coupon.entity.Coupon;
import com.urban_shop.backend.coupon.entity.CouponRedemption;
import com.urban_shop.backend.coupon.entity.DiscountType;
import com.urban_shop.backend.coupon.mapper.CouponMapper;
import com.urban_shop.backend.coupon.repository.CouponRedemptionRepository;
import com.urban_shop.backend.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private static final String COUPON_NOT_FOUND = "Cupon no encontrado";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    @Override
    @Transactional
    public CouponResponse create(UUID tenantId, CouponCreateRequest request) {
        String code = normalizeCode(request.code());
        if (couponRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new BusinessException("Ya existe un cupon con ese codigo en la tienda");
        }
        validateWindow(request.validFrom(), request.validUntil());
        validatePercentage(request.discountType(), request.discountValue());

        Coupon coupon = new Coupon();
        coupon.setTenantId(tenantId);
        coupon.setCode(code);
        apply(
            coupon,
            request.description(),
            request.discountType(),
            request.discountValue(),
            request.maxDiscountAmount(),
            request.minPurchaseAmount(),
            request.validFrom(),
            request.validUntil(),
            request.usageLimit(),
            request.perCustomerLimit()
        );
        coupon.setActive(true);
        return CouponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> list(UUID tenantId, Boolean active, int page, int size) {
        return PageResponse.from(
            couponRepository.findAdmin(
                tenantId,
                active,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).map(CouponMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse get(UUID tenantId, UUID couponId) {
        return CouponMapper.toResponse(requireCoupon(tenantId, couponId));
    }

    @Override
    @Transactional
    public CouponResponse update(UUID tenantId, UUID couponId, CouponUpdateRequest request) {
        Coupon coupon = requireCoupon(tenantId, couponId);
        validateWindow(request.validFrom(), request.validUntil());
        validatePercentage(request.discountType(), request.discountValue());
        if (request.usageLimit() != null && request.usageLimit() < coupon.getUsedCount()) {
            throw new BusinessException(
                "El limite de usos no puede ser menor que los " + coupon.getUsedCount() + " canjes ya realizados"
            );
        }

        apply(
            coupon,
            request.description(),
            request.discountType(),
            request.discountValue(),
            request.maxDiscountAmount(),
            request.minPurchaseAmount(),
            request.validFrom(),
            request.validUntil(),
            request.usageLimit(),
            request.perCustomerLimit()
        );
        coupon.setActive(Boolean.TRUE.equals(request.active()));
        return CouponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID couponId) {
        Coupon coupon = requireCoupon(tenantId, couponId);
        if (coupon.getUsedCount() > 0) {
            throw new BusinessException(
                "No se puede eliminar un cupon ya canjeado. Desactivelo en su lugar."
            );
        }
        couponRepository.delete(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponPreviewResponse preview(
        UUID tenantId,
        UUID customerId,
        String code,
        BigDecimal subtotal
    ) {
        String normalized = normalizeCode(code);
        try {
            Coupon coupon = validateForCheckout(tenantId, customerId, normalized, subtotal);
            BigDecimal discount = calculateDiscount(coupon, subtotal);
            return new CouponPreviewResponse(
                true,
                coupon.getCode(),
                subtotal,
                discount,
                subtotal.subtract(discount),
                null
            );
        } catch (BusinessException | ResourceNotFoundException ex) {
            // En la previsualizacion el rechazo es informacion, no un error.
            return CouponPreviewResponse.rejected(normalized, subtotal, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateForCheckout(
        UUID tenantId,
        UUID customerId,
        String code,
        BigDecimal subtotal
    ) {
        Coupon coupon = couponRepository.findByTenantIdAndCodeIgnoreCase(tenantId, normalizeCode(code))
            .orElseThrow(() -> new ResourceNotFoundException(COUPON_NOT_FOUND));

        if (!coupon.isActive()) {
            throw new BusinessException("El cupon no esta activo");
        }
        if (!coupon.isWithinValidityWindow(LocalDateTime.now())) {
            throw new BusinessException("El cupon no esta vigente");
        }
        if (!coupon.hasRemainingUses()) {
            throw new BusinessException("El cupon alcanzo su limite de canjes");
        }
        if (subtotal.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            throw new BusinessException(
                "El cupon requiere una compra minima de S/ " + coupon.getMinPurchaseAmount()
            );
        }
        long usedByCustomer = redemptionRepository.countByCouponIdAndCustomerId(coupon.getId(), customerId);
        if (usedByCustomer >= coupon.getPerCustomerLimit()) {
            throw new BusinessException("Ya usaste este cupon el maximo de veces permitido");
        }
        return coupon;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
            ? subtotal.multiply(coupon.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
            : coupon.getDiscountValue();

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        // El descuento nunca puede dejar el pedido en negativo.
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void redeem(
        UUID tenantId,
        UUID customerId,
        UUID orderId,
        Coupon coupon,
        BigDecimal discount
    ) {
        // Se relee bajo bloqueo: entre la validacion y el canje otro checkout pudo agotarlo.
        Coupon locked = couponRepository.findByTenantIdAndIdForUpdate(tenantId, coupon.getId())
            .orElseThrow(() -> new ResourceNotFoundException(COUPON_NOT_FOUND));
        if (!locked.hasRemainingUses()) {
            throw new BusinessException("El cupon alcanzo su limite de canjes");
        }

        locked.setUsedCount(locked.getUsedCount() + 1);
        couponRepository.save(locked);

        CouponRedemption redemption = new CouponRedemption();
        redemption.setTenantId(tenantId);
        redemption.setCouponId(locked.getId());
        redemption.setCustomerId(customerId);
        redemption.setOrderId(orderId);
        redemption.setDiscountAmount(discount);
        redemptionRepository.save(redemption);
    }

    @Override
    @Transactional
    public void releaseForOrder(UUID tenantId, UUID orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            couponRepository.findByTenantIdAndIdForUpdate(tenantId, redemption.getCouponId())
                .ifPresent(coupon -> {
                    coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
                    couponRepository.save(coupon);
                });
            redemptionRepository.delete(redemption);
            log.info("Cupon liberado por cancelacion del pedido {}", orderId);
        });
    }

    private void apply(
        Coupon coupon,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minPurchaseAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer usageLimit,
        Integer perCustomerLimit
    ) {
        coupon.setDescription(trimToNull(description));
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMaxDiscountAmount(discountType == DiscountType.PERCENTAGE ? maxDiscountAmount : null);
        coupon.setMinPurchaseAmount(minPurchaseAmount == null ? BigDecimal.ZERO : minPurchaseAmount);
        coupon.setValidFrom(validFrom);
        coupon.setValidUntil(validUntil);
        coupon.setUsageLimit(usageLimit);
        coupon.setPerCustomerLimit(perCustomerLimit == null ? 1 : perCustomerLimit);
    }

    private void validateWindow(LocalDateTime from, LocalDateTime until) {
        if (from != null && until != null && !from.isBefore(until)) {
            throw new BusinessException("La vigencia del cupon es invalida: el inicio debe ser anterior al fin");
        }
    }

    private void validatePercentage(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENTAGE && value.compareTo(HUNDRED) > 0) {
            throw new BusinessException("Un descuento porcentual no puede superar el 100%");
        }
    }

    private Coupon requireCoupon(UUID tenantId, UUID couponId) {
        return couponRepository.findByTenantIdAndId(tenantId, couponId)
            .orElseThrow(() -> new ResourceNotFoundException(COUPON_NOT_FOUND));
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("El codigo del cupon es obligatorio");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
