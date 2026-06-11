package com.urban_shop.backend.template.service;

import com.urban_shop.backend.template.dto.response.StoreTemplateResponse;

import java.util.List;

public interface TemplateService {

    List<StoreTemplateResponse> listActive();

    List<StoreTemplateResponse> listAll();
}
