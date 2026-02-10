package com.dataloader.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "total_rows")
    private Long totalRows;

    @Column(name = "processed_rows")
    private Long processedRows;

    @Column(name = "failed_rows")
    private Long failedRows;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (jobId == null) jobId = UUID.randomUUID();
        if (status == null) status = JobStatus.PENDING;
        if (processedRows == null) processedRows = 0L;
        if (failedRows == null) failedRows = 0L;
        if (totalRows == null) totalRows = 0L;
    }

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, PARTIAL
    }

    public enum FileType {
        CSV, JSON
    }
}
