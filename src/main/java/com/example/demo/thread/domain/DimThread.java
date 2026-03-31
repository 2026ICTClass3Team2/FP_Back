package com.example.demo.thread.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "dim_thread")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class DimThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dim_thread_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_solved")
    @ColumnDefault("false")
    private Boolean isSolved;

    @OneToOne(mappedBy = "dimThread", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FactThread factThread;

    @Builder
    public DimThread(String title, String content){
        this.title = title; this.content = content;
    }

    //    비지니스 로직


    // 댓글 채택 시 해결 상태로 변경하는 메소드
    public void markAsSolved(){
        this.isSolved = true;
    }

}
