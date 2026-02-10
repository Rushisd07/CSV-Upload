package com.dataloader.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Utility for JDBC batch inserts - bypasses JPA overhead for bulk operations.
 * Uses Spring JdbcTemplate.batchUpdate for best performance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JdbcBatchInserter {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Execute a parameterized batch insert.
     *
     * @param sql       parameterized SQL with ?
     * @param items     list of items to insert
     * @param setter    lambda to set PreparedStatement params from item
     * @param batchSize chunk size for sub-batching
     */
    public <T> int batchInsert(String sql, List<T> items, BiConsumer<PreparedStatement, T> setter, int batchSize) {
        if (items == null || items.isEmpty()) return 0;

        int totalInserted = 0;
        int start = 0;

        while (start < items.size()) {
            int end = Math.min(start + batchSize, items.size());
            List<T> chunk = items.subList(start, end);

            int[][] counts = jdbcTemplate.batchUpdate(sql, chunk, chunk.size(), (ps, item) -> setter.accept(ps, item));
            totalInserted += sumArray(counts);
            start = end;
        }

        return totalInserted;
    }

    /**
     * Execute an UPSERT batch (INSERT ... ON CONFLICT DO UPDATE).
     */
    public <T> int batchUpsert(String sql, List<T> items, BiConsumer<PreparedStatement, T> setter) {
        if (items == null || items.isEmpty()) return 0;

        int[][] counts = jdbcTemplate.batchUpdate(sql, items, items.size(), (ps, item) -> setter.accept(ps, item));
        return sumArray(counts);
    }

    //    private int sumArray(int[][] counts) {
//        int sum = 0;
//        for (int c : counts) sum += (c >= 0 ? c : 0);
//        return sum;
//    }
    private int sumArray(int[][] counts) {
        int sum = 0;
        for (int[] batch : counts) {
            for (int c : batch) {
                sum += Math.max(c, 0);
            }
        }
        return sum;
    }
}
