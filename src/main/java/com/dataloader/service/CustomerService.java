package com.dataloader.service;

import com.dataloader.dto.CustomerCsvRow;
import com.dataloader.model.Customer;
import com.dataloader.repository.CustomerRepository;
import com.dataloader.util.DataValidator;
import com.dataloader.util.JdbcBatchInserter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final JdbcBatchInserter jdbcBatchInserter;
    private final DataValidator dataValidator;

    private static final String UPSERT_SQL = """
            INSERT INTO customers (customer_code, first_name, last_name, email, phone,
                date_of_birth, country, city, address, postal_code, loyalty_points, is_active,
                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (customer_code)
            DO UPDATE SET
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                email = EXCLUDED.email,
                phone = EXCLUDED.phone,
                date_of_birth = EXCLUDED.date_of_birth,
                country = EXCLUDED.country,
                city = EXCLUDED.city,
                address = EXCLUDED.address,
                postal_code = EXCLUDED.postal_code,
                loyalty_points = EXCLUDED.loyalty_points,
                is_active = EXCLUDED.is_active,
                updated_at = NOW()
            """;

    /**
     * Process a batch of customer CSV rows.
     * Returns count of [processed, failed].
     */
    @Transactional
    public int[] processBatch(List<CustomerCsvRow> rows, AtomicInteger rowOffset) {
        List<CustomerCsvRow> validRows = new ArrayList<>();
        int failedCount = 0;
        int rowNumber = rowOffset.get();

        for (CustomerCsvRow row : rows) {
            rowNumber++;
            List<String> errors = dataValidator.validateCustomerRow(row, rowNumber);
            if (errors.isEmpty()) {
                validRows.add(row);
            } else {
                failedCount++;
                log.warn("Validation errors at row {}: {}", rowNumber, errors);
            }
        }
        rowOffset.set(rowNumber);

        int inserted = 0;
        if (!validRows.isEmpty()) {
            inserted = jdbcBatchInserter.batchUpsert(UPSERT_SQL, validRows, (ps, row) -> {
                try {
                    ps.setString(1, row.getCustomerCode().trim());
                    ps.setString(2, row.getFirstName().trim());
                    ps.setString(3, row.getLastName().trim());
                    ps.setString(4, row.getEmail().trim().toLowerCase());
                    ps.setString(5, row.getPhone());

                    LocalDate dob = dataValidator.parseDate(row.getDateOfBirth());
                    ps.setDate(6, dob != null ? Date.valueOf(dob) : null);

                    ps.setString(7, row.getCountry());
                    ps.setString(8, row.getCity());
                    ps.setString(9, row.getAddress());
                    ps.setString(10, row.getPostalCode());

                    Integer loyaltyPts = dataValidator.parseInteger(row.getLoyaltyPoints());
                    ps.setInt(11, loyaltyPts != null ? loyaltyPts : 0);

                    Boolean active = dataValidator.parseBoolean(row.getIsActive());
                    ps.setBoolean(12, active != null ? active : true);
                } catch (Exception e) {
                    throw new RuntimeException("Error mapping customer row: " + e.getMessage(), e);
                }
            });
        }

        return new int[]{inserted, failedCount};
    }
}
