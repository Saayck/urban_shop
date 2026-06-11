INSERT INTO roles (name, description) VALUES
('SUPER_ADMIN', 'Administrador principal de toda la plataforma'),
('TENANT_ADMIN', 'Administrador de una tienda'),
('SALES_STAFF', 'Personal de venta de una tienda'),
('CUSTOMER', 'Cliente comprador');

INSERT INTO store_templates (name, code, description) VALUES
('Urban Dark', 'URBAN_DARK', 'Plantilla oscura para marcas urbanas streetwear'),
('Minimal White', 'MINIMAL_WHITE', 'Plantilla clara, limpia y moderna'),
('Street Color', 'STREET_COLOR', 'Plantilla con colores fuertes y estilo juvenil'),
('Lookbook', 'LOOKBOOK', 'Plantilla visual enfocada en colecciones'),
('Promo Store', 'PROMO_STORE', 'Plantilla enfocada en ofertas y ventas rápidas');
