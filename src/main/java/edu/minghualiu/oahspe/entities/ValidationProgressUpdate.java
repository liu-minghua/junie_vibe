package edu.minghualiu.oahspe.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Entity tracking progress updates during async validation processing.
 */
@Entity
@Table(name = "validation_progress")
public class ValidationProgressUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ValidationRequest validationRequest;

    @Column(name = "current_item", nullable = false)
    private Integer currentItem;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public ValidationProgressUpdate() {
    }

    public ValidationProgressUpdate(ValidationRequest validationRequest, Integer currentItem, 
                                   Integer totalItems, LocalDateTime timestamp) {
        this.validationRequest = validationRequest;
        this.currentItem = currentItem;
        this.totalItems = totalItems;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ValidationRequest getValidationRequest() {
        return validationRequest;
    }

    public void setValidationRequest(ValidationRequest validationRequest) {
        this.validationRequest = validationRequest;
    }

    public Integer getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Integer currentItem) {
        this.currentItem = currentItem;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getPercentComplete() {
        if (totalItems == 0) {
            return 0;
        }
        return (currentItem * 100) / totalItems;
    }

    @Override
    public String toString() {
        return "ValidationProgressUpdate{" +
                "id=" + id +
                ", currentItem=" + currentItem +
                ", totalItems=" + totalItems +
                ", percentComplete=" + getPercentComplete() +
                ", timestamp=" + timestamp +
                '}';
    }
}
