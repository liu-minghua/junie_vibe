package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books",
    indexes = {
        @Index(name = "idx_book_page", columnList = "page_number")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;
    private String description;
    private String titleInChinese;
    private String descriptionInChinese;
    
    @Column(name = "page_number")
    private Integer pageNumber;

    @Builder.Default @OneToMany(mappedBy = "book", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<Chapter> chapters = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

}
