package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notes",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"noteKey"})},
    indexes = {
        @Index(name = "idx_note_page", columnList = "page_number")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String noteKey;
    private String text;
    private String textInChinese;
    
    @Column(name = "page_number")
    private Integer pageNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verse_id", nullable = true)
    private Verse verse;

    @Builder.Default
    @ManyToMany
    @JoinTable( name = "note_images", joinColumns = @JoinColumn(name = "note_id"), inverseJoinColumns = @JoinColumn(name = "image_id") )
    private List<Image> images = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
