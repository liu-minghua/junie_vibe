package edu.minghualiu.springmvc.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(schema = "books")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Book {
    @Id
    private Integer id;

    private String title;
    private String description;
    private String titleInChinese;
    private String descriptionInChinese;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

}
