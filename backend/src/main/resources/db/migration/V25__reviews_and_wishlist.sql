-- Resenas de producto. Solo puede resenar quien compro el producto y lo recibio,
-- y una sola vez por producto.
CREATE TABLE product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,

    rating SMALLINT NOT NULL,
    title VARCHAR(150),
    comment TEXT,

    -- El admin puede ocultar una resena abusiva sin borrar el registro.
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    moderation_note VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_product_reviews_product_same_tenant
        FOREIGN KEY (tenant_id, product_id)
        REFERENCES products (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT fk_product_reviews_order_same_tenant
        FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT uq_product_reviews_one_per_customer UNIQUE (product_id, customer_id),
    CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_product_reviews_title_not_blank
        CHECK (title IS NULL OR BTRIM(title) <> '')
);

CREATE INDEX idx_product_reviews_product_visible
    ON product_reviews (product_id, is_visible);

CREATE INDEX idx_product_reviews_tenant_created_at
    ON product_reviews (tenant_id, created_at DESC);

-- Lista de deseos del cliente.
CREATE TABLE wishlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wishlist_items_product_same_tenant
        FOREIGN KEY (tenant_id, product_id)
        REFERENCES products (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT uq_wishlist_items_customer_product UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_wishlist_items_customer
    ON wishlist_items (customer_id, created_at DESC);
