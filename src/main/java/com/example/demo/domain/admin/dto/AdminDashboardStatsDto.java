package com.example.demo.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {
    private long totalUsers;
    private long pendingReports;
    private long unseenSuggestions;
    private long totalChannels;
    private long totalFeedPosts;
    private long totalQnaPosts;
}
