package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for validation issue details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
