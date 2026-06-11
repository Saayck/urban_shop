package com.urban_shop.backend.template.repository;

import com.urban_shop.backend.template.entity.StoreTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreTemplateRepository extends JpaRepository<StoreTemplate, UUID> {

    Optional<StoreTemplate> findByCode(String code);

    List<StoreTemplate> findByActiveTrueOrderByNameAsc();
}
