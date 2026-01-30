package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for validation result with all issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
