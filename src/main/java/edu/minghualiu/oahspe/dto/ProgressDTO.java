package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for validation progress information.
 */
public class ProgressDTO {

    @JsonProperty("current")
    private Integer current;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("percent_complete")
    private Integer percentComplete;

    // Constructors
    public ProgressDTO() {
    }

    public ProgressDTO(Integer current, Integer total) {
        this.current = current;
        this.total = total;
        this.percentComplete = (total == 0) ? 0 : (current * 100) / total;
    }

    // Getters and Setters
    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(Integer percentComplete) {
        this.percentComplete = percentComplete;
    }

    @Override
    public String toString() {
        return "ProgressDTO{" +
                "current=" + current +
                ", total=" + total +
                ", percentComplete=" + percentComplete +
                '}';
    }
}
