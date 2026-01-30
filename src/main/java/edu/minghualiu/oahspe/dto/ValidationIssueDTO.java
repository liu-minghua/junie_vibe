package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO for validation issue details.
 */
public class ValidationIssueDTO {

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("entity_id")
    private Integer entityId;

    @JsonProperty("rule")
    private String rule;

    @JsonProperty("message")
    private String message;

    @JsonProperty("suggested_fix")
    private String suggestedFix;

    // Constructors
    public ValidationIssueDTO() {
    }

    public ValidationIssueDTO(String severity, String entityType, Integer entityId,
                             String rule, String message, String suggestedFix) {
        this.severity = severity;
        this.entityType = entityType;
        this.entityId = entityId;
        this.rule = rule;
        this.message = message;
        this.suggestedFix = suggestedFix;
    }

    // Getters and Setters
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
        return "ValidationIssueDTO{" +
                "severity='" + severity + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", rule='" + rule + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
