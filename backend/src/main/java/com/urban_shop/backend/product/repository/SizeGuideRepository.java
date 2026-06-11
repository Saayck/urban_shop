package com.urban_shop.backend.product.repository;

import com.urban_shop.backend.product.entity.SizeGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SizeGuideRepository extends JpaRepository<SizeGuide, UUID> {

    List<SizeGuide> findAllByProductIdOrderBySizeAsc(UUID productId);

    Optional<SizeGuide> findByIdAndProductId(UUID id, UUID productId);

    boolean existsByProductIdAndSizeIgnoreCase(UUID productId, String size);

    boolean existsByProductIdAndSizeIgnoreCaseAndIdNot(UUID productId, String size, UUID id);
}
