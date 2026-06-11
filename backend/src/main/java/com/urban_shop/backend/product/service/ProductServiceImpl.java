package com.urban_shop.backend.product.service;

import com.urban_shop.backend.brand.entity.Brand;
import com.urban_shop.backend.brand.repository.BrandRepository;
import com.urban_shop.backend.category.entity.Category;
import com.urban_shop.backend.category.repository.CategoryRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.util.SlugUtils;
import com.urban_shop.backend.inventory.service.InventoryService;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductImageRequest;
import com.urban_shop.backend.product.dto.request.ProductUpdateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantUpdateRequest;
import com.urban_shop.backend.product.dto.request.SizeGuideRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductImageResponse;
import com.urban_shop.backend.product.dto.response.ProductResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.dto.response.SizeGuideResponse;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductImage;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.product.entity.SizeGuide;
import com.urban_shop.backend.product.mapper.ProductMapper;
import com.urban_shop.backend.product.repository.ProductImageRepository;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.product.repository.ProductVariantRepository;
import com.urban_shop.backend.product.repository.SizeGuideRepository;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final List<ProductStatus> PUBLIC_STATUSES = List.of(
        ProductStatus.ACTIVE,
        ProductStatus.OUT_OF_STOCK
    );

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductVariantRepository variantRepository;
    private final SizeGuideRepository sizeGuideRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PublicTenantResolver publicTenantResolver;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public ProductDetailResponse create(UUID tenantId, ProductCreateRequest request) {
        CatalogReferences references = requireReferences(tenantId, request.brandId(), request.categoryId());
        String name = request.name().trim();
        String slug = SlugUtils.normalizeOrGenerate(request.slug(), name);
        ensureSlugAvailable(tenantId, slug, null);
        validatePrices(request.basePrice(), request.salePrice());

        Product product = new Product();
        product.setTenantId(tenantId);
        applyProduct(
            product,
            references,
            name,
            slug,
            request.description(),
            request.material(),
            request.fitType(),
            request.basePrice(),
            request.salePrice(),
            Boolean.TRUE.equals(request.featured()),
            Boolean.TRUE.equals(request.newProduct())
        );
        Product saved = productRepository.save(product);
        return toDetail(saved, references.brand(), references.category(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listAdmin(
        UUID tenantId,
        ProductStatus status,
        UUID brandId,
        UUID categoryId,
        int page,
        int size
    ) {
        Page<Product> products = productRepository.findAdmin(
            tenantId,
            status,
            brandId,
            categoryId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapProductPage(tenantId, products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdmin(UUID tenantId, UUID productId) {
        Product product = requireProduct(tenantId, productId);
        CatalogReferences references = requireReferences(tenantId, product.getBrandId(), product.getCategoryId());
        return toDetail(product, references.brand(), references.category(), false);
    }

    @Override
    @Transactional
    public ProductDetailResponse update(UUID tenantId, UUID productId, ProductUpdateRequest request) {
        Product product = requireProduct(tenantId, productId);
        CatalogReferences references = requireReferences(tenantId, request.brandId(), request.categoryId());
        String name = request.name().trim();
        String slug = SlugUtils.normalizeOrGenerate(request.slug(), name);
        ensureSlugAvailable(tenantId, slug, productId);
        validatePrices(request.basePrice(), request.salePrice());

        applyProduct(
            product,
            references,
            name,
            slug,
            request.description(),
            request.material(),
            request.fitType(),
            request.basePrice(),
            request.salePrice(),
            request.featured(),
            request.newProduct()
        );
        if (isPublished(product)) {
            validatePublishable(product, references);
        }
        return toDetail(productRepository.save(product), references.brand(), references.category(), false);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateStatus(UUID tenantId, UUID productId, ProductStatus status) {
        Product product = requireProduct(tenantId, productId);
        CatalogReferences references = requireReferences(tenantId, product.getBrandId(), product.getCategoryId());

        if (status == ProductStatus.OUT_OF_STOCK) {
            throw new BusinessException("OUT_OF_STOCK se administra automaticamente segun el stock");
        }
        if (status == ProductStatus.ACTIVE) {
            validatePublishable(product, references);
            int totalStock = activeStock(productId);
            product.setStatus(totalStock > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK);
        } else {
            product.setStatus(status);
        }
        return toDetail(productRepository.save(product), references.brand(), references.category(), false);
    }

    @Override
    @Transactional
    public ProductImageResponse addImage(UUID tenantId, UUID productId, ProductImageRequest request) {
        requireProduct(tenantId, productId);
        List<ProductImage> currentImages = imageRepository
            .findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(productId);
        boolean main = currentImages.isEmpty() || Boolean.TRUE.equals(request.main());
        if (main && !currentImages.isEmpty()) {
            imageRepository.clearMainImage(productId);
        }

        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setImageUrl(request.imageUrl().trim());
        image.setAltText(trimToNull(request.altText()));
        image.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        image.setMain(main);
        return ProductMapper.toImage(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteImage(UUID tenantId, UUID productId, UUID imageId) {
        requireProduct(tenantId, productId);
        ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada"));
        boolean wasMain = image.isMain();
        imageRepository.delete(image);
        imageRepository.flush();

        if (wasMain) {
            imageRepository.findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(productId).stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setMain(true);
                    imageRepository.save(next);
                });
        }
    }

    @Override
    @Transactional
    public ProductVariantResponse addVariant(
        UUID tenantId,
        UUID productId,
        ProductVariantCreateRequest request
    ) {
        Product product = requireProduct(tenantId, productId);
        String size = normalizeSize(request.size());
        String color = request.color().trim();
        ensureVariantAvailable(productId, size, color, null);

        ProductVariant variant = new ProductVariant();
        variant.setProductId(productId);
        variant.setSku(normalizeSku(request.sku()));
        variant.setSize(size);
        variant.setColor(color);
        variant.setColorHex(normalizeHex(request.colorHex()));
        variant.setStock(0);
        variant.setPrice(request.price());
        variant.setActive(request.active() == null || request.active());
        ProductVariant saved = variantRepository.saveAndFlush(variant);

        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        inventoryService.recordInitialStock(tenantId, saved.getId(), initialStock);
        synchronizeProductStatus(product);
        return ProductMapper.toVariant(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(
        UUID tenantId,
        UUID productId,
        UUID variantId,
        ProductVariantUpdateRequest request
    ) {
        Product product = requireProduct(tenantId, productId);
        ProductVariant variant = requireVariant(tenantId, productId, variantId);
        String size = normalizeSize(request.size());
        String color = request.color().trim();
        ensureVariantAvailable(productId, size, color, variantId);

        if (!request.active() && isPublished(product) && isLastActiveVariant(productId, variantId)) {
            throw new BusinessException("Un producto publicado debe conservar al menos una variante activa");
        }

        variant.setSku(normalizeSku(request.sku()));
        variant.setSize(size);
        variant.setColor(color);
        variant.setColorHex(normalizeHex(request.colorHex()));
        variant.setPrice(request.price());
        variant.setActive(request.active());
        ProductVariant saved = variantRepository.save(variant);
        synchronizeProductStatus(product);
        return ProductMapper.toVariant(saved);
    }

    @Override
    @Transactional
    public SizeGuideResponse addSizeGuide(UUID tenantId, UUID productId, SizeGuideRequest request) {
        requireProduct(tenantId, productId);
        String size = normalizeSize(request.size());
        if (sizeGuideRepository.existsByProductIdAndSizeIgnoreCase(productId, size)) {
            throw new BusinessException("Ya existe una guia de medidas para esa talla");
        }

        SizeGuide guide = new SizeGuide();
        guide.setProductId(productId);
        applySizeGuide(guide, request, size);
        return ProductMapper.toSizeGuide(sizeGuideRepository.save(guide));
    }

    @Override
    @Transactional
    public SizeGuideResponse updateSizeGuide(
        UUID tenantId,
        UUID productId,
        UUID guideId,
        SizeGuideRequest request
    ) {
        requireProduct(tenantId, productId);
        SizeGuide guide = sizeGuideRepository.findByIdAndProductId(guideId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Guia de medidas no encontrada"));
        String size = normalizeSize(request.size());
        if (sizeGuideRepository.existsByProductIdAndSizeIgnoreCaseAndIdNot(productId, size, guideId)) {
            throw new BusinessException("Ya existe una guia de medidas para esa talla");
        }
        applySizeGuide(guide, request, size);
        return ProductMapper.toSizeGuide(sizeGuideRepository.save(guide));
    }

    @Override
    @Transactional
    public void deleteSizeGuide(UUID tenantId, UUID productId, UUID guideId) {
        requireProduct(tenantId, productId);
        SizeGuide guide = sizeGuideRepository.findByIdAndProductId(guideId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Guia de medidas no encontrada"));
        sizeGuideRepository.delete(guide);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listPublic(
        String tenantSlug,
        UUID brandId,
        UUID categoryId,
        int page,
        int size
    ) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        Page<Product> products = productRepository.findPublic(
            tenantId,
            PUBLIC_STATUSES,
            brandId,
            categoryId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return mapProductPage(tenantId, products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getPublic(String tenantSlug, String productSlug) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        String normalizedSlug;
        try {
            normalizedSlug = SlugUtils.normalizeOrGenerate(productSlug, productSlug);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
        Product product = productRepository.findByTenantIdAndSlugAndStatusIn(
                tenantId,
                normalizedSlug,
                PUBLIC_STATUSES
            )
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        CatalogReferences references = requireReferences(tenantId, product.getBrandId(), product.getCategoryId());
        if (references.category() == null || !references.category().isActive()) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
        if (references.brand() != null && !references.brand().isActive()) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
        return toDetail(product, references.brand(), references.category(), true);
    }

    private PageResponse<ProductResponse> mapProductPage(UUID tenantId, Page<Product> products) {
        if (products.isEmpty()) {
            return new PageResponse<>(
                List.of(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
            );
        }
        List<UUID> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<UUID, List<ProductImage>> images = imageRepository
            .findAllByProductIdInOrderByDisplayOrderAscCreatedAtAsc(productIds)
            .stream()
            .collect(Collectors.groupingBy(ProductImage::getProductId));
        Map<UUID, List<ProductVariant>> variants = variantRepository.findAllByProductIdIn(productIds)
            .stream()
            .collect(Collectors.groupingBy(ProductVariant::getProductId));
        Map<UUID, String> brandNames = brandRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
            .collect(Collectors.toMap(Brand::getId, Brand::getName));
        Map<UUID, String> categoryNames = categoryRepository
            .findAllByTenantIdOrderByDisplayOrderAscNameAsc(tenantId)
            .stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

        Page<ProductResponse> mapped = products.map(product -> {
            List<ProductImage> productImages = images.getOrDefault(product.getId(), List.of());
            List<ProductVariant> productVariants = variants.getOrDefault(product.getId(), List.of());
            String mainImage = productImages.stream()
                .filter(ProductImage::isMain)
                .findFirst()
                .or(() -> productImages.stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
            int totalStock = productVariants.stream()
                .filter(ProductVariant::isActive)
                .mapToInt(ProductVariant::getStock)
                .sum();
            return ProductMapper.toResponse(
                product,
                brandNames.get(product.getBrandId()),
                categoryNames.get(product.getCategoryId()),
                mainImage,
                totalStock
            );
        });
        return PageResponse.from(mapped);
    }

    private ProductDetailResponse toDetail(
        Product product,
        Brand brand,
        Category category,
        boolean publicView
    ) {
        List<ProductImage> images = imageRepository
            .findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(product.getId());
        List<ProductVariant> variants = variantRepository
            .findAllByProductIdOrderBySizeAscColorAsc(product.getId());
        if (publicView) {
            variants = variants.stream().filter(ProductVariant::isActive).toList();
        }
        List<SizeGuide> guides = sizeGuideRepository.findAllByProductIdOrderBySizeAsc(product.getId());
        return ProductMapper.toDetail(
            product,
            brand == null ? null : brand.getName(),
            category == null ? null : category.getName(),
            images,
            variants,
            guides
        );
    }

    private void validatePublishable(Product product, CatalogReferences references) {
        if (references.category() == null || !references.category().isActive()) {
            throw new BusinessException("El producto necesita una categoria activa para publicarse");
        }
        if (references.brand() != null && !references.brand().isActive()) {
            throw new BusinessException("La marca debe estar activa para publicar el producto");
        }
        boolean hasActiveVariant = variantRepository
            .findAllByProductIdOrderBySizeAscColorAsc(product.getId())
            .stream()
            .anyMatch(ProductVariant::isActive);
        if (!hasActiveVariant) {
            throw new BusinessException("El producto necesita al menos una variante activa para publicarse");
        }
    }

    private void synchronizeProductStatus(Product product) {
        if (!isPublished(product)) {
            return;
        }
        int stock = activeStock(product.getId());
        ProductStatus expected = stock > 0 ? ProductStatus.ACTIVE : ProductStatus.OUT_OF_STOCK;
        if (product.getStatus() != expected) {
            product.setStatus(expected);
            productRepository.save(product);
        }
    }

    private int activeStock(UUID productId) {
        return variantRepository.findAllByProductIdOrderBySizeAscColorAsc(productId).stream()
            .filter(ProductVariant::isActive)
            .mapToInt(ProductVariant::getStock)
            .sum();
    }

    private boolean isLastActiveVariant(UUID productId, UUID excludedVariantId) {
        return variantRepository.findAllByProductIdOrderBySizeAscColorAsc(productId).stream()
            .filter(variant -> !variant.getId().equals(excludedVariantId))
            .noneMatch(ProductVariant::isActive);
    }

    private Product requireProduct(UUID tenantId, UUID productId) {
        return productRepository.findByTenantIdAndId(tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private ProductVariant requireVariant(UUID tenantId, UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository.findByTenantIdAndId(tenantId, variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
        if (!variant.getProductId().equals(productId)) {
            throw new ResourceNotFoundException("Variante no encontrada");
        }
        return variant;
    }

    private CatalogReferences requireReferences(UUID tenantId, UUID brandId, UUID categoryId) {
        Brand brand = brandId == null ? null : brandRepository.findByTenantIdAndId(tenantId, brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        Category category = categoryId == null ? null : categoryRepository.findByTenantIdAndId(tenantId, categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        return new CatalogReferences(brand, category);
    }

    private void ensureSlugAvailable(UUID tenantId, String slug, UUID currentId) {
        boolean exists = currentId == null
            ? productRepository.existsByTenantIdAndSlug(tenantId, slug)
            : productRepository.existsByTenantIdAndSlugAndIdNot(tenantId, slug, currentId);
        if (exists) {
            throw new BusinessException("Ya existe un producto con ese slug en la tienda");
        }
    }

    private void ensureVariantAvailable(UUID productId, String size, String color, UUID currentId) {
        boolean exists = currentId == null
            ? variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(productId, size, color)
            : variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCaseAndIdNot(
                productId,
                size,
                color,
                currentId
            );
        if (exists) {
            throw new BusinessException("Ya existe una variante con esa talla y color");
        }
    }

    private void validatePrices(BigDecimal basePrice, BigDecimal salePrice) {
        if (salePrice != null && salePrice.compareTo(basePrice) > 0) {
            throw new BusinessException("El precio de oferta no puede superar el precio base");
        }
    }

    private void applyProduct(
        Product product,
        CatalogReferences references,
        String name,
        String slug,
        String description,
        String material,
        String fitType,
        BigDecimal basePrice,
        BigDecimal salePrice,
        boolean featured,
        boolean newProduct
    ) {
        product.setBrandId(references.brand() == null ? null : references.brand().getId());
        product.setCategoryId(references.category() == null ? null : references.category().getId());
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(trimToNull(description));
        product.setMaterial(trimToNull(material));
        product.setFitType(fitType == null || fitType.isBlank() ? null : fitType.trim().toUpperCase(Locale.ROOT));
        product.setBasePrice(basePrice);
        product.setSalePrice(salePrice);
        product.setFeatured(featured);
        product.setNewProduct(newProduct);
    }

    private void applySizeGuide(SizeGuide guide, SizeGuideRequest request, String size) {
        guide.setSize(size);
        guide.setChestCm(request.chestCm());
        guide.setWaistCm(request.waistCm());
        guide.setHipCm(request.hipCm());
        guide.setLengthCm(request.lengthCm());
        guide.setShoulderCm(request.shoulderCm());
        guide.setSleeveCm(request.sleeveCm());
        guide.setInseamCm(request.inseamCm());
        guide.setNotes(trimToNull(request.notes()));
    }

    private boolean isPublished(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE
            || product.getStatus() == ProductStatus.OUT_OF_STOCK;
    }

    private String normalizeSize(String size) {
        return size.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeHex(String colorHex) {
        return colorHex == null || colorHex.isBlank() ? null : colorHex.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CatalogReferences(Brand brand, Category category) {
    }
}
