package com.dataloader.service;

import com.dataloader.dto.OrderCsvRow;
import com.dataloader.model.Customer;
import com.dataloader.model.Order;
import com.dataloader.model.Product;
import com.dataloader.repository.CustomerRepository;
import com.dataloader.repository.OrderRepository;
import com.dataloader.repository.ProductRepository;
import com.dataloader.util.DataValidator;
import com.dataloader.util.JdbcBatchInserter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final JdbcBatchInserter jdbcBatchInserter;
    private final JdbcTemplate jdbcTemplate;
    private final DataValidator dataValidator;

    // Caches to avoid repeated lookups during bulk processing
    private final Map<String, Long> customerCache = new ConcurrentHashMap<>();
    private final Map<String, Long> productCache  = new ConcurrentHashMap<>();

    private static final String ORDER_UPSERT_SQL = """
            INSERT INTO orders (order_number, customer_id, status, total_amount,
                discount_amount, tax_amount, shipping_amount, currency,
                shipping_address, notes, ordered_at, shipped_at, delivered_at,
                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (order_number)
            DO UPDATE SET
                status = EXCLUDED.status,
                total_amount = EXCLUDED.total_amount,
                updated_at = NOW()
            RETURNING id, order_number
            """;

    private static final String ORDER_ITEM_INSERT_SQL = """
            INSERT INTO order_items (order_id, product_id, quantity, unit_price, discount, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
            ON CONFLICT DO NOTHING
            """;

    /**
     * Process a batch of order rows.
     * Groups rows by order_number, upserts orders, then inserts order items.
     */
    @Transactional
    public int[] processBatch(List<OrderCsvRow> rows, AtomicInteger rowOffset) {
        List<OrderCsvRow> validRows = new ArrayList<>();
        int failedCount = 0;
        int rowNumber = rowOffset.get();

        for (OrderCsvRow row : rows) {
            rowNumber++;
            List<String> errors = dataValidator.validateOrderRow(row, rowNumber);
            if (errors.isEmpty()) {
                validRows.add(row);
            } else {
                failedCount++;
                log.warn("Order validation errors at row {}: {}", rowNumber, errors);
            }
        }
        rowOffset.set(rowNumber);

        if (validRows.isEmpty()) return new int[]{0, failedCount};

        // Group by order number
        Map<String, List<OrderCsvRow>> orderGroups = validRows.stream()
                .collect(Collectors.groupingBy(r -> r.getOrderNumber().trim(),
                         LinkedHashMap::new, Collectors.toList()));

        int insertedOrders = 0;
        int insertedItems  = 0;

        for (Map.Entry<String, List<OrderCsvRow>> entry : orderGroups.entrySet()) {
            String orderNumber = entry.getKey();
            List<OrderCsvRow> orderRows = entry.getValue();
            OrderCsvRow first = orderRows.get(0);

            try {
                Long customerId = resolveCustomerId(first.getCustomerCode());
                if (customerId == null) {
                    log.warn("Customer not found for code '{}', skipping order '{}'",
                             first.getCustomerCode(), orderNumber);
                    failedCount += orderRows.size();
                    continue;
                }

                // Upsert order - get back the order ID
                Long orderId = upsertOrder(first, customerId);
                if (orderId == null) {
                    failedCount += orderRows.size();
                    continue;
                }
                insertedOrders++;

                // Insert order items
                for (OrderCsvRow itemRow : orderRows) {
                    Long productId = resolveProductId(itemRow.getProductCode());
                    if (productId == null) {
                        log.warn("Product not found: '{}', skipping item for order '{}'",
                                 itemRow.getProductCode(), orderNumber);
                        failedCount++;
                        continue;
                    }
                    insertOrderItem(orderId, productId, itemRow);
                    insertedItems++;
                }

            } catch (Exception e) {
                log.error("Error processing order '{}': {}", orderNumber, e.getMessage());
                failedCount += orderRows.size();
            }
        }

        log.debug("Batch: {} orders, {} items inserted, {} failed", insertedOrders, insertedItems, failedCount);
        return new int[]{insertedOrders + insertedItems, failedCount};
    }

    private Long upsertOrder(OrderCsvRow row, Long customerId) {
        try {
            String status = dataValidator.isBlank(row.getStatus()) ? "PENDING" : row.getStatus().trim().toUpperCase();
            BigDecimal total    = safeDecimal(row.getTotalAmount(),    BigDecimal.ZERO);
            BigDecimal discount = safeDecimal(row.getDiscountAmount(), BigDecimal.ZERO);
            BigDecimal tax      = safeDecimal(row.getTaxAmount(),      BigDecimal.ZERO);
            BigDecimal shipping = safeDecimal(row.getShippingAmount(), BigDecimal.ZERO);
            String currency     = dataValidator.isBlank(row.getCurrency()) ? "USD" : row.getCurrency().trim().toUpperCase();

            LocalDateTime orderedAt  = dataValidator.parseDateTime(row.getOrderedAt());
            LocalDateTime shippedAt  = dataValidator.parseDateTime(row.getShippedAt());
            LocalDateTime deliveredAt = dataValidator.parseDateTime(row.getDeliveredAt());

            // Use simple INSERT ... ON CONFLICT and then fetch the ID
            jdbcTemplate.update("""
                    INSERT INTO orders (order_number, customer_id, status, total_amount,
                        discount_amount, tax_amount, shipping_amount, currency,
                        shipping_address, notes, ordered_at, shipped_at, delivered_at,
                        created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON CONFLICT (order_number) DO UPDATE SET
                        status = EXCLUDED.status,
                        total_amount = EXCLUDED.total_amount,
                        updated_at = NOW()
                    """,
                    row.getOrderNumber().trim(), customerId, status, total, discount, tax, shipping,
                    currency, row.getShippingAddress(), row.getNotes(),
                    orderedAt  != null ? Timestamp.valueOf(orderedAt)   : Timestamp.valueOf(LocalDateTime.now()),
                    shippedAt  != null ? Timestamp.valueOf(shippedAt)   : null,
                    deliveredAt!= null ? Timestamp.valueOf(deliveredAt) : null
            );

            return jdbcTemplate.queryForObject(
                    "SELECT id FROM orders WHERE order_number = ?",
                    Long.class, row.getOrderNumber().trim());

        } catch (Exception e) {
            log.error("Failed to upsert order '{}': {}", row.getOrderNumber(), e.getMessage());
            return null;
        }
    }

    private void insertOrderItem(Long orderId, Long productId, OrderCsvRow row) {
        BigDecimal unitPrice = safeDecimal(row.getUnitPrice(), BigDecimal.ZERO);
        BigDecimal discount  = safeDecimal(row.getItemDiscount(), BigDecimal.ZERO);
        Integer quantity     = dataValidator.parseInteger(row.getQuantity());
        if (quantity == null || quantity <= 0) quantity = 1;

        jdbcTemplate.update(
                "INSERT INTO order_items (order_id, product_id, quantity, unit_price, discount, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())",
                orderId, productId, quantity, unitPrice, discount
        );
    }

    private Long resolveCustomerId(String customerCode) {
        if (dataValidator.isBlank(customerCode)) return null;
        String code = customerCode.trim();
        return customerCache.computeIfAbsent(code, c ->
                customerRepository.findByCustomerCode(c).map(Customer::getId).orElse(null));
    }

    private Long resolveProductId(String productCode) {
        if (dataValidator.isBlank(productCode)) return null;
        String code = productCode.trim();
        return productCache.computeIfAbsent(code, c ->
                productRepository.findByProductCode(c).map(Product::getId).orElse(null));
    }

    private BigDecimal safeDecimal(String value, BigDecimal defaultValue) {
        BigDecimal parsed = dataValidator.parseDecimal(value);
        return parsed != null ? parsed : defaultValue;
    }

    public void clearCaches() {
        customerCache.clear();
        productCache.clear();
    }
}
