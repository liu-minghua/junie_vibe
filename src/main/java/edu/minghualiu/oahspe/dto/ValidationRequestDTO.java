package edu.minghualiu.oahspe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for validation request submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
