package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for validation status response.
 */
public class ValidationStatusDTO {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("progress")
    private ProgressDTO progress;

    @JsonProperty("started_at")
    private LocalDateTime startedAt;

    @JsonProperty("completed_at")
    private LocalDateTime completedAt;

    @JsonProperty("result")
    private ValidationResultDTO result;

    // Constructors
    public ValidationStatusDTO() {
    }

    public ValidationStatusDTO(String requestId, String status, ProgressDTO progress,
                              LocalDateTime startedAt) {
        this.requestId = requestId;
        this.status = status;
        this.progress = progress;
        this.startedAt = startedAt;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ProgressDTO getProgress() {
        return progress;
    }

    public void setProgress(ProgressDTO progress) {
        this.progress = progress;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public ValidationResultDTO getResult() {
        return result;
    }

    public void setResult(ValidationResultDTO result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ValidationStatusDTO{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
