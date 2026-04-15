package com.example.demo.domain.qna.dto;

import lombok.Data;
import java.util.List;

@Data
public class QnaCreateRequestDto {
    private String title;
    private String body;
    private List<String> tags;
    private int rewardPoints;
}
