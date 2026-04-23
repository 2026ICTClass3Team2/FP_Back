package com.example.demo.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @Column(name = "resource_id")
    private Long id;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden;
}
