package com.example.demo.domain.content.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tag",
        indexes = {
                @Index(name = "idx_tag_name", columnList = "tag_name")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String name;
}
