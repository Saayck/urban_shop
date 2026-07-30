-- Cupones de descuento. Hasta ahora orders.discount_total existia pero siempre valia 0
-- porque no habia forma de aplicar un descuento.
CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    code VARCHAR(50) NOT NULL,
    description VARCHAR(255),

    discount_type VARCHAR(20) NOT NULL,
    -- Porcentaje (1-100) o monto fijo en soles, segun discount_type.
    discount_value NUMERIC(10, 2) NOT NULL,
    -- Tope de descuento para los cupones porcentuales.
    max_discount_amount NUMERIC(10, 2),
    -- Compra minima para que el cupon aplique.
    min_purchase_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,

    valid_from TIMESTAMP,
    valid_until TIMESTAMP,

    -- NULL = sin limite de canjes.
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    -- Cuantas veces puede usarlo un mismo cliente.
    per_customer_limit INTEGER NOT NULL DEFAULT 1,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_coupons_code_per_tenant UNIQUE (tenant_id, code),
    CONSTRAINT ck_coupons_code_not_blank CHECK (BTRIM(code) <> ''),
    CONSTRAINT ck_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupons_percentage_range
        CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT ck_coupons_max_discount CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT ck_coupons_min_purchase CHECK (min_purchase_amount >= 0),
    CONSTRAINT ck_coupons_usage_limit CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT ck_coupons_used_count CHECK (used_count >= 0),
    CONSTRAINT ck_coupons_per_customer_limit CHECK (per_customer_limit > 0),
    CONSTRAINT ck_coupons_validity_window
        CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until)
);

CREATE INDEX idx_coupons_tenant_active ON coupons (tenant_id, is_active);

-- Un canje por pedido. La unicidad (order_id) impide contar dos veces el mismo pedido.
CREATE TABLE coupon_redemptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    coupon_id UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    order_id UUID NOT NULL UNIQUE,

    discount_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_coupon_redemptions_order_same_tenant
        FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT ck_coupon_redemptions_amount CHECK (discount_amount >= 0)
);

CREATE INDEX idx_coupon_redemptions_coupon_customer
    ON coupon_redemptions (coupon_id, customer_id);

-- Referencia del cupon aplicado en el pedido, para poder mostrarlo en el detalle.
ALTER TABLE orders
    ADD COLUMN coupon_id UUID REFERENCES coupons(id),
    ADD COLUMN coupon_code VARCHAR(50);
