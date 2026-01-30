package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;

/**
 * Entity storing individual validation issues from a validation result.
 */
@Entity
@Table(name = "validation_issue_records")
public class ValidationIssueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ValidationResultEntity validationResult;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "rule", nullable = false)
    private String rule;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "suggested_fix")
    private String suggestedFix;

    // Constructors
    public ValidationIssueRecord() {
    }

    public ValidationIssueRecord(String severity, String entityType, Integer entityId,
                                String rule, String message, String suggestedFix) {
        this.severity = severity;
        this.entityType = entityType;
        this.entityId = entityId;
        this.rule = rule;
        this.message = message;
        this.suggestedFix = suggestedFix;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ValidationResultEntity getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResultEntity validationResult) {
        this.validationResult = validationResult;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    @Override
    public String toString() {
        return "ValidationIssueRecord{" +
                "id=" + id +
                ", severity='" + severity + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", rule='" + rule + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
