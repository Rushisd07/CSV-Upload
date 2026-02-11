package com.dataloader.controller;

//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import com.dataloader.dto.ApiResponse;
import com.dataloader.dto.UploadJobResponse;
import com.dataloader.model.UploadJob;
import com.dataloader.service.FileUploadService;
import com.dataloader.service.UploadJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/upload")
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UploadJobService  uploadJobService;

    public FileUploadController(FileUploadService fileUploadService, UploadJobService uploadJobService) {
		super();
		this.fileUploadService = fileUploadService;
		this.uploadJobService = uploadJobService;
	}

	// -------------------------------------------------------
    // CSV UPLOAD
    // -------------------------------------------------------
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadJobResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") FileUploadService.DataType dataType) {

        validateFile(file, "text/csv", ".csv");

        UploadJob job = fileUploadService.initiateUpload(file, dataType, UploadJob.FileType.CSV);

        Path path = Paths.get("uploads")
                .resolve(job.getJobId() + "_" + file.getOriginalFilename());
        // Process asynchronously - returns immediately with job ID
        fileUploadService.processCsvAsync(job.getJobId(), path, dataType);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "File accepted for processing. Track progress using the jobId.",
                        UploadJobResponse.from(job)));
    }

    // -------------------------------------------------------
    // JSON UPLOAD
    // -------------------------------------------------------
    @PostMapping(value = "/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadJobResponse>> uploadJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") FileUploadService.DataType dataType) {

        validateFile(file, "application/json", ".json");

        UploadJob job = fileUploadService.initiateUpload(file, dataType, UploadJob.FileType.JSON);

        Path path = Paths.get("uploads")
                .resolve(job.getJobId() + "_" + file.getOriginalFilename());
        fileUploadService.processJsonAsync(job.getJobId(), path, dataType);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "File accepted for processing. Track progress using the jobId.",
                        UploadJobResponse.from(job)));
    }

    // -------------------------------------------------------
    // JOB STATUS
    // -------------------------------------------------------
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<UploadJobResponse>> getJobStatus(
            @PathVariable UUID jobId) {

        UploadJobResponse response = uploadJobService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success("Job status retrieved", response));
    }

    // -------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------
    private void validateFile(MultipartFile file, String contentType, String extension) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or missing.");
        }
        if (file.getOriginalFilename() != null &&
            !file.getOriginalFilename().toLowerCase().endsWith(extension)) {
            log.warn("File '{}' might not be a {}", file.getOriginalFilename(), extension);
            // Warning only - don't reject based on extension alone
        }
    }
}
