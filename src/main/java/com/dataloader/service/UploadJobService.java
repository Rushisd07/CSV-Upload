package com.dataloader.service;

import com.dataloader.dto.UploadJobResponse;
import com.dataloader.model.UploadJob;
import com.dataloader.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadJobService {

    private final UploadJobRepository uploadJobRepository;

    @Transactional
    public UploadJob createJob(String fileName, UploadJob.FileType fileType) {
        UploadJob job = UploadJob.builder()
                .jobId(UUID.randomUUID())
                .fileName(fileName)
                .fileType(fileType)
                .status(UploadJob.JobStatus.PENDING)
                .totalRows(0L)
                .processedRows(0L)
                .failedRows(0L)
                .build();
        return uploadJobRepository.save(job);
    }

    @Transactional
    public void markProcessing(UUID jobId) {
        uploadJobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus(UploadJob.JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            uploadJobRepository.save(job);
        });
    }

    @Transactional
    public void updateProgress(UUID jobId, long totalRows, long processedRows, long failedRows) {
        uploadJobRepository.updateProgress(jobId, UploadJob.JobStatus.PROCESSING,
                processedRows, failedRows, totalRows);
    }

    @Transactional
    public void markCompleted(UUID jobId, long totalRows, long processedRows, long failedRows) {
        uploadJobRepository.findByJobId(jobId).ifPresent(job -> {
            UploadJob.JobStatus finalStatus = failedRows == 0
                    ? UploadJob.JobStatus.COMPLETED
                    : (processedRows == 0 ? UploadJob.JobStatus.FAILED : UploadJob.JobStatus.PARTIAL);
            job.setStatus(finalStatus);
            job.setTotalRows(totalRows);
            job.setProcessedRows(processedRows);
            job.setFailedRows(failedRows);
            job.setCompletedAt(LocalDateTime.now());
            uploadJobRepository.save(job);
        });
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        uploadJobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus(UploadJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            uploadJobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public UploadJobResponse getJobStatus(UUID jobId) {
        return uploadJobRepository.findByJobId(jobId)
                .map(UploadJobResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
}
