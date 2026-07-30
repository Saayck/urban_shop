package com.urban_shop.backend.common.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Deserializador que devuelve el texto tal cual, sin pasar por {@link XssSanitizer}.
 * <p>
 * {@link XssSanitizingDeserializer} esta registrado globalmente para todos los String,
 * lo que es correcto para texto libre que luego se muestra en la tienda, pero destruiria
 * silenciosamente credenciales y tokens que contengan secuencias como {@code eval(...)}
 * o {@code javascript:}. Esos campos deben anotarse con
 * {@code @JsonDeserialize(using = RawStringDeserializer.class)}.
 */
public class RawStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return parser.getValueAsString();
    }
}
