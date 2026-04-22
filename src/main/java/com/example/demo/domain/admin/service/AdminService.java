package com.example.demo.domain.admin.service;

import com.example.demo.domain.admin.dto.AdminDashboardStatsDto;
import com.example.demo.domain.admin.dto.AdminUserDto;
import com.example.demo.domain.admin.dto.AdminChannelDto;
import com.example.demo.domain.admin.dto.ReportAdminDto;
import com.example.demo.domain.admin.dto.SuspendRequestDto;
import com.example.demo.domain.suggestion.entity.Suggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AdminService {
    AdminDashboardStatsDto getDashboardStats();
    
    // User Management
    Page<AdminUserDto> searchUsers(String keyword, String status, Pageable pageable);
    void warnUser(Long userId, Long adminId);
    void suspendUser(Long userId, Long adminId, SuspendRequestDto requestDto);
    
    // Report Management
    Page<ReportAdminDto> getReports(String status, Pageable pageable);
    void updateReportStatus(Long reportId, String status);
    Map<String, Object> getReportTargetDetails(Long reportId);
    void deleteComment(Long commentId);
    void hidePost(Long postId);
    void hideChannel(Long channelId);
    
    // Channel Management
    Page<AdminChannelDto> searchChannels(String keyword, String status, Pageable pageable);
    void updateChannelStatus(Long channelId, String status);
    
    // Suggestion Management
    Page<Suggestion> getSuggestions(Boolean isSeen, Pageable pageable);
    void markSuggestionAsSeen(Long suggestionId);
}
