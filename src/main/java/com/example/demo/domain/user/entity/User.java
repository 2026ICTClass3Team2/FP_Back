package com.example.demo.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "roleList")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nickname;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_role_list", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private List<String> roleList = new ArrayList<>();

    public void addRole(String role) {
        roleList.add(role);
    }

    public void clearRole() {
        roleList.clear();
    }
}
