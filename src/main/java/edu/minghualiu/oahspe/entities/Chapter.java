package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapters",
    indexes = {
        @Index(name = "idx_chapter_page", columnList = "page_number")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String titleInChinese;
    private String descriptionInChinese;
    
    @Column(name = "page_number")
    private Integer pageNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.PERSIST, orphanRemoval = false)
    @Builder.Default
    private List<Verse> verses = new ArrayList<>();


    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
