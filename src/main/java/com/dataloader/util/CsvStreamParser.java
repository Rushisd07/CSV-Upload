package com.dataloader.util;

import com.dataloader.dto.CustomerCsvRow;
import com.dataloader.dto.OrderCsvRow;
import com.dataloader.dto.ProductCsvRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Component
public class CsvStreamParser {

    public void streamCustomers(InputStream inputStream,
                                int batchSize,
                                Consumer<List<CustomerCsvRow>> batchConsumer,
                                Consumer<Long> totalRowCounter) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), 64 * 1024);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<CustomerCsvRow> batch = new ArrayList<>(batchSize);
            long totalRows = 0;

            for (CSVRecord record : parser) {
                CustomerCsvRow row = CustomerCsvRow.builder()
                        .customerCode(safeGet(record, "customerCode"))
                        .firstName(safeGet(record, "firstName"))
                        .lastName(safeGet(record, "lastName"))
                        .email(safeGet(record, "email"))
                        .phone(safeGet(record, "phone"))
                        .dateOfBirth(safeGet(record, "dateOfBirth"))
                        .country(safeGet(record, "country"))
                        .city(safeGet(record, "city"))
                        .address(safeGet(record, "address"))
                        .postalCode(safeGet(record, "postalCode"))
                        .loyaltyPoints(safeGet(record, "loyaltyPoints"))
                        .isActive(safeGet(record, "isActive"))
                        .build();

                batch.add(row);
                totalRows++;

                if (batch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }

            totalRowCounter.accept(totalRows);
        }
    }

    public void streamProducts(InputStream inputStream,
                               int batchSize,
                               Consumer<List<ProductCsvRow>> batchConsumer,
                               Consumer<Long> totalRowCounter) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), 64 * 1024);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<ProductCsvRow> batch = new ArrayList<>(batchSize);
            long totalRows = 0;

            for (CSVRecord record : parser) {
                ProductCsvRow row = ProductCsvRow.builder()
                        .productCode(safeGet(record, "productCode"))
                        .productName(safeGet(record, "productName"))
                        .description(safeGet(record, "description"))
                        .categoryCode(safeGet(record, "categoryCode"))
                        .unitPrice(safeGet(record, "unitPrice"))
                        .stockQuantity(safeGet(record, "stockQuantity"))
                        .weightKg(safeGet(record, "weightKg"))
                        .brand(safeGet(record, "brand"))
                        .sku(safeGet(record, "sku"))
                        .isActive(safeGet(record, "isActive"))
                        .build();

                batch.add(row);
                totalRows++;

                if (batch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }

            totalRowCounter.accept(totalRows);
        }
    }

    public void streamOrders(InputStream inputStream,
                             int batchSize,
                             Consumer<List<OrderCsvRow>> batchConsumer,
                             Consumer<Long> totalRowCounter) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), 64 * 1024);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<OrderCsvRow> batch = new ArrayList<>(batchSize);
            long totalRows = 0;

            for (CSVRecord record : parser) {
                OrderCsvRow row = OrderCsvRow.builder()
                        .orderNumber(safeGet(record, "orderNumber"))
                        .customerCode(safeGet(record, "customerCode"))
                        .status(safeGet(record, "status"))
                        .totalAmount(safeGet(record, "totalAmount"))
                        .discountAmount(safeGet(record, "discountAmount"))
                        .taxAmount(safeGet(record, "taxAmount"))
                        .shippingAmount(safeGet(record, "shippingAmount"))
                        .currency(safeGet(record, "currency"))
                        .shippingAddress(safeGet(record, "shippingAddress"))
                        .notes(safeGet(record, "notes"))
                        .orderedAt(safeGet(record, "orderedAt"))
                        .shippedAt(safeGet(record, "shippedAt"))
                        .deliveredAt(safeGet(record, "deliveredAt"))
                        .productCode(safeGet(record, "productCode"))
                        .quantity(safeGet(record, "quantity"))
                        .unitPrice(safeGet(record, "unitPrice"))
                        .itemDiscount(safeGet(record, "itemDiscount"))
                        .build();

                batch.add(row);
                totalRows++;

                if (batch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }

            totalRowCounter.accept(totalRows);
        }
    }

    // -------------------------------------------------------
    // HEADER VALIDATION
    // -------------------------------------------------------
    public Set<String> extractHeaders(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(false)
                     .build()
                     .parse(reader)) {

            return new HashSet<>(parser.getHeaderNames());
        }
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            return record.isMapped(column) ? record.get(column) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
