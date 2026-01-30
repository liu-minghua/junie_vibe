package edu.minghualiu.oahspe.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

/**
 * Entity representing a validation request submitted via the REST API.
 * Tracks the status, progress, and results of async validation operations.
 */
@Entity
@Table(name = "validation_requests")
public class ValidationRequest {

    @Id
    @Column(name = "request_id")
    private String requestId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ValidationStatus status;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "verse_id")
    private Integer verseId;

    @Column(name = "validate_notes")
    private Boolean validateNotes;

    @Column(name = "validate_images")
    private Boolean validateImages;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "validationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValidationProgressUpdate> progressUpdates = new ArrayList<>();

    @OneToOne(mappedBy = "validationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ValidationResultEntity result;

    // Constructors
    public ValidationRequest() {
    }

    public ValidationRequest(String requestId, ValidationStatus status, Integer bookId, 
                            Integer chapterId, Integer verseId, Boolean validateNotes, 
                            Boolean validateImages, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.status = status;
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.verseId = verseId;
        this.validateNotes = validateNotes;
        this.validateImages = validateImages;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationStatus status) {
        this.status = status;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getVerseId() {
        return verseId;
    }

    public void setVerseId(Integer verseId) {
        this.verseId = verseId;
    }

    public Boolean getValidateNotes() {
        return validateNotes;
    }

    public void setValidateNotes(Boolean validateNotes) {
        this.validateNotes = validateNotes;
    }

    public Boolean getValidateImages() {
        return validateImages;
    }

    public void setValidateImages(Boolean validateImages) {
        this.validateImages = validateImages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<ValidationProgressUpdate> getProgressUpdates() {
        return progressUpdates;
    }

    public void setProgressUpdates(List<ValidationProgressUpdate> progressUpdates) {
        this.progressUpdates = progressUpdates;
    }

    public ValidationResultEntity getResult() {
        return result;
    }

    public void setResult(ValidationResultEntity result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ValidationRequest{" +
                "requestId='" + requestId + '\'' +
                ", status=" + status +
                ", bookId=" + bookId +
                ", chapterId=" + chapterId +
                ", verseId=" + verseId +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
