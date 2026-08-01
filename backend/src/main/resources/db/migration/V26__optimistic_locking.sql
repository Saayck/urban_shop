-- Bloqueo optimista sobre las entidades que edita el panel de administracion.
-- Sin esto, dos ediciones simultaneas del mismo producto o cupon se pisan en silencio:
-- gana la ultima en escribir y los cambios de la otra se pierden sin aviso.
--
-- No se aplica a tablas de solo-insercion (pedidos, pagos, movimientos de inventario,
-- auditoria, tokens): ahi no hay ediciones concurrentes que perder, y los puntos
-- criticos ya usan bloqueo pesimista.
ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE brands ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE categories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tenants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
