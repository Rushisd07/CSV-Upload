package com.dataloader.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    @NotBlank(message = "Product code is required")
    private String productCode;

    @Column(name = "product_name", nullable = false)
    @NotBlank(message = "Product name is required")
    private String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false)
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (stockQuantity == null) stockQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
