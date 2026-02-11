package com.dataloader.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCsvRow {
    // Order fields
    private String orderNumber;
    private String customerCode;
    private String status;
    private String totalAmount;
    private String discountAmount;
    private String taxAmount;
    private String shippingAmount;
    private String currency;
    private String shippingAddress;
    private String notes;
    private String orderedAt;
    private String shippedAt;
    private String deliveredAt;

    // Order Item fields (one row = one order item)
    private String productCode;
    private String quantity;
    private String unitPrice;
    private String itemDiscount;
}
