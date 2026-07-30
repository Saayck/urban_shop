package com.urban_shop.backend.media.controller;

import com.urban_shop.backend.media.dto.MediaResponse;
import com.urban_shop.backend.media.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
@Tag(name = "Admin Media", description = "Endpoints de carga y gestion de archivos multimedia")
@RequiredArgsConstructor
public class AdminMediaController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Subir imagen o archivo multimedia", description = "Sube una imagen (JPEG, PNG, WEBP, GIF, SVG) y retorna la URL publica del archivo almacenado.")
    public ResponseEntity<MediaResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        MediaResponse response = storageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF', 'SUPER_ADMIN')")
    @Operation(
        summary = "Eliminar un archivo subido",
        description = "Borra el archivo fisico a partir de su URL publica (/uploads/...). "
            + "Sirve para descartar una subida que no llego a asociarse a ningun producto. "
            + "Las imagenes ya asociadas se borran solas al eliminar la imagen del producto."
    )
    public ResponseEntity<Void> deleteFile(@RequestParam("url") String url) {
        return storageService.deleteByPublicUrl(url)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
