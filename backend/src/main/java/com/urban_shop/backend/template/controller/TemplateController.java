package com.urban_shop.backend.template.controller;

import com.urban_shop.backend.template.dto.response.StoreTemplateResponse;
import com.urban_shop.backend.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<StoreTemplateResponse> list() {
        return templateService.listActive();
    }
}
