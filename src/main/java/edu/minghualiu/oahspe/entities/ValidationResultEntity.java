package edu.minghualiu.oahspe.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

/**
 * Entity storing the final validation result for a validation request.
 */
@Entity
@Table(name = "validation_results")
public class ValidationResultEntity {

    @Id
    @Column(name = "request_id")
    private String requestId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    private ValidationRequest validationRequest;

    @Column(name = "total_issues", nullable = false)
    private Integer totalIssues;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "warning_count", nullable = false)
    private Integer warningCount;

    @Column(name = "info_count", nullable = false)
    private Integer infoCount;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "validationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValidationIssueRecord> issues = new ArrayList<>();

    // Constructors
    public ValidationResultEntity() {
    }

    public ValidationResultEntity(String requestId, Integer totalIssues, Integer errorCount,
                                 Integer warningCount, Integer infoCount, LocalDateTime completedAt) {
        this.requestId = requestId;
        this.totalIssues = totalIssues;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ValidationRequest getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(ValidationRequest validationRequest) {
        this.validationRequest = validationRequest;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<ValidationIssueRecord> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssueRecord> issues) {
        this.issues = issues;
    }

    @Override
    public String toString() {
        return "ValidationResultEntity{" +
                "requestId='" + requestId + '\'' +
                ", totalIssues=" + totalIssues +
                ", errorCount=" + errorCount +
                ", warningCount=" + warningCount +
                ", infoCount=" + infoCount +
                ", completedAt=" + completedAt +
                '}';
    }
}
