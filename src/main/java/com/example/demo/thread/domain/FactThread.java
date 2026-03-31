package com.example.demo.thread.domain;

import com.example.demo.user.domain.DimUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Table(name = "fact_thread")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
public class FactThread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fact_thread_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_thread_id", nullable = false)
    private DimThread dimThread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dim_user_id", nullable = false)
    private DimUser dimUser;

    @Column(name = "thread_points")
    @ColumnDefault("0")
    private Integer threadPoints;

    @Column(name = "like_count")
    @ColumnDefault("0")
    private Integer likeCount;

    @Column(name = "dislike_count")
    @ColumnDefault("0")
    private Integer dislikeCount;

    @Column(name = "view_count")
    @ColumnDefault("0")
    private Integer viewCount;

    @Column(name = "bookmark_count")
    @ColumnDefault("0")
    private Integer bookmarkCount;

    @Column(name = "comment_count")
    @ColumnDefault("0")
    private Integer commentCount;

    @Column(name = "pinned_comment_id")
    private Integer pinnedCommentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FactThread(DimThread dimThread, DimUser dimUser, Integer threadPoints){
        this.dimThread = dimThread; this.dimUser = dimUser; this.threadPoints = threadPoints;
    }

    //    비지니스 로직



    public void addViewCount(){
        this.viewCount++;
    }

    public void pinComment(Integer commentId){
        this.pinnedCommentId = commentId;
    }

}
