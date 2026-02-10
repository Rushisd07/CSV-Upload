package com.dataloader.util;

import com.dataloader.dto.CustomerCsvRow;
import com.dataloader.dto.OrderCsvRow;
import com.dataloader.dto.ProductCsvRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DataValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    // -------------------------------------------------------
    // CSV HEADER VALIDATION
    // -------------------------------------------------------
    public static final Set<String> CUSTOMER_REQUIRED_HEADERS = Set.of(
            "customerCode", "firstName", "lastName", "email"
    );

    public static final Set<String> PRODUCT_REQUIRED_HEADERS = Set.of(
            "productCode", "productName", "unitPrice"
    );

    public static final Set<String> ORDER_REQUIRED_HEADERS = Set.of(
            "orderNumber", "customerCode", "productCode", "quantity", "unitPrice"
    );

    public List<String> validateCustomerRow(CustomerCsvRow row, int rowNumber) {
        List<String> errors = new ArrayList<>();

        if (isBlank(row.getCustomerCode())) {
            errors.add(String.format("Row %d: customerCode is required", rowNumber));
        }
        if (isBlank(row.getFirstName())) {
            errors.add(String.format("Row %d: firstName is required", rowNumber));
        }
        if (isBlank(row.getLastName())) {
            errors.add(String.format("Row %d: lastName is required", rowNumber));
        }
        if (isBlank(row.getEmail())) {
            errors.add(String.format("Row %d: email is required", rowNumber));
        } else if (!EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            errors.add(String.format("Row %d: invalid email format '%s'", rowNumber, row.getEmail()));
        }
        if (!isBlank(row.getLoyaltyPoints()) && !isValidInteger(row.getLoyaltyPoints())) {
            errors.add(String.format("Row %d: loyaltyPoints must be a valid integer", rowNumber));
        }
        if (!isBlank(row.getDateOfBirth()) && parseDate(row.getDateOfBirth()) == null) {
            errors.add(String.format("Row %d: invalid dateOfBirth format '%s'", rowNumber, row.getDateOfBirth()));
        }

        return errors;
    }

    public List<String> validateProductRow(ProductCsvRow row, int rowNumber) {
        List<String> errors = new ArrayList<>();

        if (isBlank(row.getProductCode())) {
            errors.add(String.format("Row %d: productCode is required", rowNumber));
        }
        if (isBlank(row.getProductName())) {
            errors.add(String.format("Row %d: productName is required", rowNumber));
        }
        if (isBlank(row.getUnitPrice())) {
            errors.add(String.format("Row %d: unitPrice is required", rowNumber));
        } else if (!isValidPositiveDecimal(row.getUnitPrice())) {
            errors.add(String.format("Row %d: unitPrice must be a positive decimal value", rowNumber));
        }
        if (!isBlank(row.getStockQuantity()) && !isValidNonNegativeInteger(row.getStockQuantity())) {
            errors.add(String.format("Row %d: stockQuantity must be a non-negative integer", rowNumber));
        }
        if (!isBlank(row.getWeightKg()) && !isValidPositiveDecimal(row.getWeightKg())) {
            errors.add(String.format("Row %d: weightKg must be a positive decimal", rowNumber));
        }

        return errors;
    }

    public List<String> validateOrderRow(OrderCsvRow row, int rowNumber) {
        List<String> errors = new ArrayList<>();

        if (isBlank(row.getOrderNumber())) {
            errors.add(String.format("Row %d: orderNumber is required", rowNumber));
        }
        if (isBlank(row.getCustomerCode())) {
            errors.add(String.format("Row %d: customerCode is required", rowNumber));
        }
        if (isBlank(row.getProductCode())) {
            errors.add(String.format("Row %d: productCode is required", rowNumber));
        }
        if (isBlank(row.getQuantity())) {
            errors.add(String.format("Row %d: quantity is required", rowNumber));
        } else if (!isValidPositiveInteger(row.getQuantity())) {
            errors.add(String.format("Row %d: quantity must be a positive integer", rowNumber));
        }
        if (isBlank(row.getUnitPrice())) {
            errors.add(String.format("Row %d: unitPrice is required", rowNumber));
        } else if (!isValidPositiveDecimal(row.getUnitPrice())) {
            errors.add(String.format("Row %d: unitPrice must be positive", rowNumber));
        }

        return errors;
    }

    // -------------------------------------------------------
    // PARSING HELPERS
    // -------------------------------------------------------
    public LocalDate parseDate(String value) {
        if (isBlank(value)) return null;
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) return null;
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Try parsing as date only, return start of day
        LocalDate date = parseDate(trimmed);
        return date != null ? date.atStartOfDay() : null;
    }

    public BigDecimal parseDecimal(String value) {
        if (isBlank(value)) return null;
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer parseInteger(String value) {
        if (isBlank(value)) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean parseBoolean(String value) {
        if (isBlank(value)) return null;
        return switch (value.trim().toLowerCase()) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> null;
        };
    }

    // -------------------------------------------------------
    // VALIDATION PRIMITIVES
    // -------------------------------------------------------
    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidInteger(String value) {
        try { Integer.parseInt(value.trim()); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private boolean isValidNonNegativeInteger(String value) {
        try { return Integer.parseInt(value.trim()) >= 0; }
        catch (NumberFormatException e) { return false; }
    }

    private boolean isValidPositiveInteger(String value) {
        try { return Integer.parseInt(value.trim()) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    private boolean isValidPositiveDecimal(String value) {
        try { return new BigDecimal(value.trim()).compareTo(BigDecimal.ZERO) >= 0; }
        catch (NumberFormatException e) { return false; }
    }
}
