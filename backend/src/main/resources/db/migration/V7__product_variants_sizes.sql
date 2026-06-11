CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,

    sku VARCHAR(100),
    size VARCHAR(30) NOT NULL,
    color VARCHAR(50) NOT NULL,
    color_hex VARCHAR(20),

    stock INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_variant_per_product UNIQUE (product_id, size, color)
);

CREATE TABLE size_guides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,

    size VARCHAR(30) NOT NULL,

    chest_cm DECIMAL(6,2),
    waist_cm DECIMAL(6,2),
    hip_cm DECIMAL(6,2),
    length_cm DECIMAL(6,2),
    shoulder_cm DECIMAL(6,2),
    sleeve_cm DECIMAL(6,2),
    inseam_cm DECIMAL(6,2),

    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_size_guide_per_product UNIQUE (product_id, size)
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL REFERENCES product_variants(id),

    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    reason TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
