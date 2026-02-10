package com.dataloader.dto;

import com.dataloader.model.UploadJob;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadJobResponse {

    private UUID jobId;
    private String fileName;
    private String fileType;
    private String status;
    private Long totalRows;
    private Long processedRows;
    private Long failedRows;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private Double progressPercent;

    public static UploadJobResponse from(UploadJob job) {
        double progress = 0.0;
        if (job.getTotalRows() != null && job.getTotalRows() > 0) {
            progress = (job.getProcessedRows() * 100.0) / job.getTotalRows();
        }

        return UploadJobResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .fileType(job.getFileType() != null ? job.getFileType().name() : null)
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .failedRows(job.getFailedRows())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .progressPercent(Math.round(progress * 100.0) / 100.0)
                .build();
    }
}
