package com.dataloader.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCsvRow {
    private String productCode;
    private String productName;
    private String description;
    private String categoryCode;
    private String unitPrice;
    private String stockQuantity;
    private String weightKg;
    private String brand;
    private String sku;
    private String isActive;
}
