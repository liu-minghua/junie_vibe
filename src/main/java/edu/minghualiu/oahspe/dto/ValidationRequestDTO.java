package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for validation request submission.
 */
public class ValidationRequestDTO {

    @JsonProperty("book_id")
    private Integer bookId;

    @JsonProperty("chapter_id")
    private Integer chapterId;

    @JsonProperty("verse_id")
    private Integer verseId;

    @JsonProperty("validate_notes")
    private Boolean validateNotes;

    @JsonProperty("validate_images")
    private Boolean validateImages;

    // Constructors
    public ValidationRequestDTO() {
    }

    public ValidationRequestDTO(Integer bookId, Integer chapterId, Integer verseId,
                               Boolean validateNotes, Boolean validateImages) {
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.verseId = verseId;
        this.validateNotes = validateNotes;
        this.validateImages = validateImages;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "ValidationRequestDTO{" +
                "bookId=" + bookId +
                ", chapterId=" + chapterId +
                ", verseId=" + verseId +
                ", validateNotes=" + validateNotes +
                ", validateImages=" + validateImages +
                '}';
    }
}
