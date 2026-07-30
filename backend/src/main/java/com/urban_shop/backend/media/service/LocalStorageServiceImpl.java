package com.urban_shop.backend.media.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.media.dto.MediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");
    private static final String PUBLIC_PREFIX = "/uploads/";
    private final Path uploadLocation;

    public LocalStorageServiceImpl(@Value("${app.storage.location:uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", this.uploadLocation, e);
            throw new BusinessException("No se pudo inicializar el directorio de almacenamiento de archivos");
        }
    }

    @Override
    public MediaResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo subido esta vacio");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = getExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Formato de archivo no permitido. Solo se aceptan: " + ALLOWED_EXTENSIONS);
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destinationFile = this.uploadLocation.resolve(storedFilename).normalize();

        if (!destinationFile.getParent().equals(this.uploadLocation)) {
            throw new BusinessException("Intento de navegacion de directorio no valido");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully: {} -> {}", originalFilename, storedFilename);
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new BusinessException("Error al guardar el archivo en el servidor");
        }

        String publicUrl = PUBLIC_PREFIX + storedFilename;
        return new MediaResponse(storedFilename, publicUrl, file.getContentType(), file.getSize());
    }

    @Override
    public boolean deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            // URL externa (CDN) o vacia: no hay nada nuestro que borrar.
            return false;
        }

        String filename = StringUtils.cleanPath(publicUrl.substring(PUBLIC_PREFIX.length()));
        if (filename.isBlank()) {
            return false;
        }

        Path target = this.uploadLocation.resolve(filename).normalize();
        // Impide que un ".." en la URL alcance archivos fuera del directorio de subidas.
        if (!target.startsWith(this.uploadLocation)) {
            log.warn("Intento de borrado fuera del directorio de subidas: {}", publicUrl);
            return false;
        }

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("Archivo eliminado: {}", filename);
            }
            return deleted;
        } catch (IOException e) {
            // Un archivo que no se pudo borrar no debe abortar la operacion de negocio.
            log.error("No se pudo eliminar el archivo {}", filename, e);
            return false;
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
