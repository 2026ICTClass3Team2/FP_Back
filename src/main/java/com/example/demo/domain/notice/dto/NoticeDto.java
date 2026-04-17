package com.example.demo.domain.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDto {

    private Long postId;          // post_id (BIGINT)
    private String title;         // title (VARCHAR)
    private String body;          // body (TEXT) - 리액트의 content가 여기로!
    private String author;        // author (VARCHAR) - "관리자"로 고정

    // ENUM 타입들은 일단 String으로 받아서 처리
    private String contentType;   // content_type (기본값: notice 로 고정)
    private String sourceType;    // source_type (기본값: internal)
    private String status;        // status (기본값: active)

    private int viewCount;        // view_count (INT)

    // 외래키 관련
    private Long channelId;       // channel_id (BIGINT)
    private Long userId;          // user_id (BIGINT)
}