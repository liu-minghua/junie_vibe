package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "images",
uniqueConstraints = {
@UniqueConstraint(columnNames = {"imageKey"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 50)
    private String imageKey;

    @Column(nullable = false)
    private String title;
    private String titleInChinese;
    @Column(nullable = false)
    private String description;
    private String descriptionInChines;
    private Integer sourcePage;
    private String originalFilename;
  	
    private String contentType;
    @Lob @Column(columnDefinition = "BLOB")
    private byte[] data;

    @Builder.Default
    @ManyToMany(mappedBy = "images")
    private List<Note> notes = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
