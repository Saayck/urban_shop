ALTER TABLE brands
    DROP CONSTRAINT uq_brand_per_tenant,
    ADD CONSTRAINT ck_brands_name_not_blank
        CHECK (BTRIM(name) <> '');

CREATE UNIQUE INDEX uq_brands_name_lower_per_tenant
    ON brands (tenant_id, LOWER(name));

ALTER TABLE categories
    ALTER COLUMN display_order SET NOT NULL,
    ADD CONSTRAINT ck_categories_name_not_blank
        CHECK (BTRIM(name) <> ''),
    ADD CONSTRAINT ck_categories_slug_format
        CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    ADD CONSTRAINT ck_categories_display_order
        CHECK (display_order >= 0),
    ADD CONSTRAINT ck_categories_not_own_parent
        CHECK (parent_id IS NULL OR parent_id <> id);

ALTER TABLE products
    ADD CONSTRAINT ck_products_name_not_blank
        CHECK (BTRIM(name) <> ''),
    ADD CONSTRAINT ck_products_slug_format
        CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    ADD CONSTRAINT ck_products_base_price
        CHECK (base_price > 0),
    ADD CONSTRAINT ck_products_sale_price
        CHECK (sale_price IS NULL OR (sale_price > 0 AND sale_price <= base_price)),
    ADD CONSTRAINT ck_products_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'OUT_OF_STOCK'));

ALTER TABLE product_images
    ALTER COLUMN display_order SET NOT NULL,
    ADD CONSTRAINT ck_product_images_url_not_blank
        CHECK (BTRIM(image_url) <> ''),
    ADD CONSTRAINT ck_product_images_display_order
        CHECK (display_order >= 0);

CREATE UNIQUE INDEX uq_product_main_image
    ON product_images (product_id)
    WHERE is_main = TRUE;

ALTER TABLE product_variants
    DROP CONSTRAINT uq_variant_per_product,
    ADD CONSTRAINT ck_product_variants_size_not_blank
        CHECK (BTRIM(size) <> ''),
    ADD CONSTRAINT ck_product_variants_color_not_blank
        CHECK (BTRIM(color) <> ''),
    ADD CONSTRAINT ck_product_variants_stock
        CHECK (stock >= 0),
    ADD CONSTRAINT ck_product_variants_price
        CHECK (price IS NULL OR price > 0),
    ADD CONSTRAINT ck_product_variants_sku_not_blank
        CHECK (sku IS NULL OR BTRIM(sku) <> ''),
    ADD CONSTRAINT ck_product_variants_color_hex
        CHECK (color_hex IS NULL OR color_hex ~ '^#[A-F0-9]{6}$');

CREATE UNIQUE INDEX uq_variant_size_color_lower_per_product
    ON product_variants (product_id, LOWER(size), LOWER(color));

CREATE UNIQUE INDEX uq_variant_sku_lower_per_product
    ON product_variants (product_id, LOWER(sku))
    WHERE sku IS NOT NULL;

ALTER TABLE size_guides
    DROP CONSTRAINT uq_size_guide_per_product,
    ADD CONSTRAINT ck_size_guides_size_not_blank
        CHECK (BTRIM(size) <> ''),
    ADD CONSTRAINT ck_size_guides_measurements
        CHECK (
            (chest_cm IS NULL OR chest_cm > 0)
            AND (waist_cm IS NULL OR waist_cm > 0)
            AND (hip_cm IS NULL OR hip_cm > 0)
            AND (length_cm IS NULL OR length_cm > 0)
            AND (shoulder_cm IS NULL OR shoulder_cm > 0)
            AND (sleeve_cm IS NULL OR sleeve_cm > 0)
            AND (inseam_cm IS NULL OR inseam_cm > 0)
        );

CREATE UNIQUE INDEX uq_size_guide_lower_per_product
    ON size_guides (product_id, LOWER(size));

ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movement_type
        CHECK (movement_type IN ('ENTRY', 'EXIT', 'ADJUSTMENT', 'SALE', 'RETURN')),
    ADD CONSTRAINT ck_inventory_movement_quantity
        CHECK (quantity <> 0);

CREATE INDEX idx_product_images_product_id
    ON product_images (product_id);

CREATE INDEX idx_product_variants_product_id
    ON product_variants (product_id);

CREATE INDEX idx_size_guides_product_id
    ON size_guides (product_id);

CREATE INDEX idx_inventory_movements_tenant_created_at
    ON inventory_movements (tenant_id, created_at DESC);

CREATE INDEX idx_inventory_movements_variant_id
    ON inventory_movements (variant_id);
