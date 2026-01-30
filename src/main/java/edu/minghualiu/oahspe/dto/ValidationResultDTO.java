package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for validation result with all issues.
 */
public class ValidationResultDTO {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("total_issues")
    private Integer totalIssues;

    @JsonProperty("error_count")
    private Integer errorCount;

    @JsonProperty("warning_count")
    private Integer warningCount;

    @JsonProperty("info_count")
    private Integer infoCount;

    @JsonProperty("issues")
    private List<ValidationIssueDTO> issues;

    @JsonProperty("completed_at")
    private LocalDateTime completedAt;

    // Constructors
    public ValidationResultDTO() {
    }

    public ValidationResultDTO(String requestId, String status, Integer totalIssues,
                              Integer errorCount, Integer warningCount, Integer infoCount,
                              List<ValidationIssueDTO> issues, LocalDateTime completedAt) {
        this.requestId = requestId;
        this.status = status;
        this.totalIssues = totalIssues;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.issues = issues;
        this.completedAt = completedAt;
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

    public Integer getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(Integer totalIssues) {
        this.totalIssues = totalIssues;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public Integer getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(Integer infoCount) {
        this.infoCount = infoCount;
    }

    public List<ValidationIssueDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssueDTO> issues) {
        this.issues = issues;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "ValidationResultDTO{" +
                "requestId='" + requestId + '\'' +
                ", status='" + status + '\'' +
                ", totalIssues=" + totalIssues +
                ", errorCount=" + errorCount +
                ", warningCount=" + warningCount +
                ", infoCount=" + infoCount +
                ", completedAt=" + completedAt +
                '}';
    }
}
