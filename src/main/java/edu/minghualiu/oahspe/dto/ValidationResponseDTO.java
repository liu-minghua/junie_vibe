package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO for validation response (202 Accepted).
 */
public class ValidationResponseDTO {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Constructors
    public ValidationResponseDTO() {
    }

    public ValidationResponseDTO(String requestId, String status, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.status = status;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ValidationResponseDTO{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
