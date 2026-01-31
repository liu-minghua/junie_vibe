package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for validation progress information.
 */
@Data
@NoArgsConstructor
public class ProgressDTO {

    @JsonProperty("current")
    private Integer current;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("percent_complete")
    private Integer percentComplete;

    public ProgressDTO(Integer current, Integer total) {
        this.current = current;
        this.total = total;
        this.percentComplete = (total == 0) ? 0 : (current * 100) / total;
    }
}
