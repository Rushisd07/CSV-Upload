package com.dataloader.repository;

import com.dataloader.model.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {

    Optional<UploadJob> findByJobId(UUID jobId);

    @Modifying
    @Query("UPDATE UploadJob j SET j.status = :status, j.processedRows = :processedRows, " +
           "j.failedRows = :failedRows, j.totalRows = :totalRows WHERE j.jobId = :jobId")
    void updateProgress(@Param("jobId") UUID jobId,
                        @Param("status") UploadJob.JobStatus status,
                        @Param("processedRows") Long processedRows,
                        @Param("failedRows") Long failedRows,
                        @Param("totalRows") Long totalRows);
}
