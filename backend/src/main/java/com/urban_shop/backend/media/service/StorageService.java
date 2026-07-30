package com.urban_shop.backend.media.service;

import com.urban_shop.backend.media.dto.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    MediaResponse store(MultipartFile file);

    /**
     * Borra el archivo fisico que respalda una URL {@code /uploads/...}.
     * Es tolerante: si la URL apunta fuera del almacenamiento propio (por ejemplo a un CDN
     * externo) o el archivo ya no existe, no hace nada y no falla.
     *
     * @return true si efectivamente se elimino un archivo
     */
    boolean deleteByPublicUrl(String publicUrl);
}
