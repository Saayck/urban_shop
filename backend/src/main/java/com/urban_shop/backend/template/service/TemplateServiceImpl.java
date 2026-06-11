package com.urban_shop.backend.template.service;

import com.urban_shop.backend.template.dto.response.StoreTemplateResponse;
import com.urban_shop.backend.template.entity.StoreTemplate;
import com.urban_shop.backend.template.repository.StoreTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final StoreTemplateRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<StoreTemplateResponse> listActive() {
        return repository.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreTemplateResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private StoreTemplateResponse toResponse(StoreTemplate t) {
        return new StoreTemplateResponse(
            t.getId(),
            t.getName(),
            t.getCode(),
            t.getDescription(),
            t.getPreviewImageUrl(),
            t.isActive()
        );
    }
}
