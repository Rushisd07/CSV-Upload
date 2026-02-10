package com.dataloader.service;

import com.dataloader.dto.ProductCsvRow;
import com.dataloader.model.Category;
import com.dataloader.repository.CategoryRepository;
import com.dataloader.util.DataValidator;
import com.dataloader.util.JdbcBatchInserter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final CategoryRepository categoryRepository;
    private final JdbcBatchInserter jdbcBatchInserter;
    private final DataValidator dataValidator;

    // Category code -> ID cache (avoid repeated DB lookups)
    private final Map<String, Long> categoryCache = new ConcurrentHashMap<>();

    private static final String UPSERT_SQL = """
            INSERT INTO products (product_code, product_name, description, category_id,
                unit_price, stock_quantity, weight_kg, brand, sku, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (product_code)
            DO UPDATE SET
                product_name = EXCLUDED.product_name,
                description = EXCLUDED.description,
                category_id = EXCLUDED.category_id,
                unit_price = EXCLUDED.unit_price,
                stock_quantity = EXCLUDED.stock_quantity,
                weight_kg = EXCLUDED.weight_kg,
                brand = EXCLUDED.brand,
                sku = EXCLUDED.sku,
                is_active = EXCLUDED.is_active,
                updated_at = NOW()
            """;

    @Transactional
    public int[] processBatch(List<ProductCsvRow> rows, AtomicInteger rowOffset) {
        List<ProductCsvRow> validRows = new ArrayList<>();
        int failedCount = 0;
        int rowNumber = rowOffset.get();

        for (ProductCsvRow row : rows) {
            rowNumber++;
            List<String> errors = dataValidator.validateProductRow(row, rowNumber);
            if (errors.isEmpty()) {
                validRows.add(row);
            } else {
                failedCount++;
                log.warn("Product validation errors at row {}: {}", rowNumber, errors);
            }
        }
        rowOffset.set(rowNumber);

        int inserted = 0;
        if (!validRows.isEmpty()) {
            inserted = jdbcBatchInserter.batchUpsert(UPSERT_SQL, validRows, (ps, row) -> {
                try {
                    ps.setString(1, row.getProductCode().trim());
                    ps.setString(2, row.getProductName().trim());
                    ps.setString(3, row.getDescription());

                    Long categoryId = resolveCategoryId(row.getCategoryCode());
                    if (categoryId != null) {
                        ps.setLong(4, categoryId);
                    } else {
                        ps.setNull(4, java.sql.Types.BIGINT);
                    }

                    BigDecimal price = dataValidator.parseDecimal(row.getUnitPrice());
                    ps.setBigDecimal(5, price != null ? price : BigDecimal.ZERO);

                    Integer stock = dataValidator.parseInteger(row.getStockQuantity());
                    ps.setInt(6, stock != null ? stock : 0);

                    BigDecimal weight = dataValidator.parseDecimal(row.getWeightKg());
                    if (weight != null) ps.setBigDecimal(7, weight);
                    else ps.setNull(7, java.sql.Types.NUMERIC);

                    ps.setString(8, row.getBrand());
                    ps.setString(9, dataValidator.isBlank(row.getSku()) ? null : row.getSku().trim());

                    Boolean active = dataValidator.parseBoolean(row.getIsActive());
                    ps.setBoolean(10, active != null ? active : true);
                } catch (Exception e) {
                    throw new RuntimeException("Error mapping product row: " + e.getMessage(), e);
                }
            });
        }

        return new int[]{inserted, failedCount};
    }

    private Long resolveCategoryId(String categoryCode) {
        if (dataValidator.isBlank(categoryCode)) return null;
        String code = categoryCode.trim().toUpperCase();
        return categoryCache.computeIfAbsent(code, c -> {
            Optional<Category> category = categoryRepository.findByCategoryCode(c);
            return category.map(Category::getId).orElse(null);
        });
    }

    public void clearCategoryCache() {
        categoryCache.clear();
    }
}
