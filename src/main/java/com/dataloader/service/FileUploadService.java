package com.dataloader.service;

import com.dataloader.dto.CustomerCsvRow;
import com.dataloader.dto.OrderCsvRow;
import com.dataloader.dto.ProductCsvRow;
import com.dataloader.model.UploadJob;
import com.dataloader.util.CsvStreamParser;
import com.dataloader.util.JsonStreamParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates async file processing for CSV and JSON uploads.
 * Streams files in batches to avoid full in-memory loading.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final UploadJobService uploadJobService;
    private final CustomerService  customerService;
    private final ProductService   productService;
    private final OrderService     orderService;
    private final CsvStreamParser  csvStreamParser;
    private final JsonStreamParser jsonStreamParser;

    @Value("${app.batch.size:500}")
    private int batchSize;

    public enum DataType { CUSTOMERS, PRODUCTS, ORDERS }

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    // -------------------------------------------------------
    // ENTRY POINT: save file + create job
    // -------------------------------------------------------
    public UploadJob initiateUpload(
            MultipartFile file,
            DataType dataType,
            UploadJob.FileType fileType
    ) {
        try {
            Files.createDirectories(UPLOAD_DIR);

            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "unknown";

            UploadJob job = uploadJobService.createJob(originalName, fileType);

            Path storedPath = UPLOAD_DIR.resolve(
                    job.getJobId() + "_" + originalName
            );

            Files.copy(
                    file.getInputStream(),
                    storedPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            log.info("Stored upload file for job {} at {}", job.getJobId(), storedPath);

            return job;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }

    // -------------------------------------------------------
    // ASYNC CSV PROCESSING
    // -------------------------------------------------------
    @Async("fileProcessingExecutor")
    public void processCsvAsync(UUID jobId, Path filePath, DataType dataType) {

        uploadJobService.markProcessing(jobId);
        log.info("[Job {}] Starting CSV processing for {}", jobId, dataType);

        AtomicLong total = new AtomicLong();
        AtomicLong ok    = new AtomicLong();
        AtomicLong fail  = new AtomicLong();
        AtomicInteger offset = new AtomicInteger();

        try (InputStream is = Files.newInputStream(filePath)) {

            switch (dataType) {
                case CUSTOMERS -> csvStreamParser.streamCustomers(
                        is, batchSize,
                        batch -> {
                            int[] r = customerService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );

                case PRODUCTS -> csvStreamParser.streamProducts(
                        is, batchSize,
                        batch -> {
                            int[] r = productService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );

                case ORDERS -> csvStreamParser.streamOrders(
                        is, batchSize,
                        batch -> {
                            int[] r = orderService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );
            }

            uploadJobService.markCompleted(jobId, total.get(), ok.get(), fail.get());
            log.info("[Job {}] CSV completed. total={}, ok={}, failed={}",
                    jobId, total.get(), ok.get(), fail.get());

        } catch (Exception e) {
            log.error("[Job {}] CSV processing failed", jobId, e);
            uploadJobService.markFailed(jobId, e.getMessage());
        } finally {
            deleteQuietly(filePath);
        }
    }

    // -------------------------------------------------------
    // ASYNC JSON PROCESSING
    // -------------------------------------------------------
    @Async("fileProcessingExecutor")
    public void processJsonAsync(UUID jobId, Path filePath, DataType dataType) {

        uploadJobService.markProcessing(jobId);
        log.info("[Job {}] Starting JSON processing for {}", jobId, dataType);

        AtomicLong total = new AtomicLong();
        AtomicLong ok    = new AtomicLong();
        AtomicLong fail  = new AtomicLong();
        AtomicInteger offset = new AtomicInteger();

        try (InputStream is = Files.newInputStream(filePath)) {

            switch (dataType) {
                case CUSTOMERS -> jsonStreamParser.streamArray(
                        is, CustomerCsvRow.class, batchSize,
                        batch -> {
                            int[] r = customerService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );

                case PRODUCTS -> jsonStreamParser.streamArray(
                        is, ProductCsvRow.class, batchSize,
                        batch -> {
                            int[] r = productService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );

                case ORDERS -> jsonStreamParser.streamArray(
                        is, OrderCsvRow.class, batchSize,
                        batch -> {
                            int[] r = orderService.processBatch(batch, offset);
                            ok.addAndGet(r[0]);
                            fail.addAndGet(r[1]);
                            total.addAndGet(batch.size());
                        },
                        total::set
                );
            }

            uploadJobService.markCompleted(jobId, total.get(), ok.get(), fail.get());
            log.info("[Job {}] JSON completed. total={}, ok={}, failed={}",
                    jobId, total.get(), ok.get(), fail.get());

        } catch (Exception e) {
            log.error("[Job {}] JSON processing failed", jobId, e);
            uploadJobService.markFailed(jobId, e.getMessage());
        } finally {
            deleteQuietly(filePath);
        }
    }

    // -------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------
    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
