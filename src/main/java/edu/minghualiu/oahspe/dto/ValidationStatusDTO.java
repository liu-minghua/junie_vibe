package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for validation status response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // Custom constructor for convenience
    public ValidationStatusDTO(String requestId, String status, ProgressDTO progress,
                              LocalDateTime startedAt) {
        this.requestId = requestId;
        this.status = status;
        this.progress = progress;
        this.startedAt = startedAt;
    }
}
