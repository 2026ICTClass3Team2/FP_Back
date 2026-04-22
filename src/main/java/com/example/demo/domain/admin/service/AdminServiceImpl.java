package com.example.demo.domain.admin.service;

import com.example.demo.domain.admin.dto.AdminDashboardStatsDto;
import com.example.demo.domain.admin.dto.AdminUserDto;
import com.example.demo.domain.admin.dto.AdminChannelDto;
import com.example.demo.domain.admin.dto.ReportAdminDto;
import com.example.demo.domain.admin.dto.SuspendRequestDto;
import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.channel.entity.ChannelTag;
import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.channel.repository.ChannelTagRepository;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.repository.ReportRepository;
import com.example.demo.domain.suggestion.entity.Suggestion;
import com.example.demo.domain.suggestion.repository.SuggestionRepository;
import com.example.demo.domain.user.entity.Suspended;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import com.example.demo.domain.user.repository.SuspendedRepository;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final SuggestionRepository suggestionRepository;
    private final ChannelRepository channelRepository;
    private final ChannelTagRepository channelTagRepository;
    private final PostRepository postRepository;
    private final SuspendedRepository suspendedRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        return AdminDashboardStatsDto.builder()
                .totalUsers(userRepository.count())
                .pendingReports(reportRepository.countByStatus("pending"))
                .unseenSuggestions(suggestionRepository.countByIsSeenFalse())
                .totalChannels(channelRepository.count())
                .totalFeedPosts(postRepository.countByContentType("feed"))
                .totalQnaPosts(postRepository.countByContentType("qna"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> searchUsers(String keyword, String statusStr, Pageable pageable) {
        UserStatus status = null;
        if (statusStr != null && !statusStr.isEmpty() && !statusStr.equalsIgnoreCase("all")) {
            status = UserStatus.valueOf(statusStr.toLowerCase());
        }
        Page<User> users = userRepository.searchUsers(keyword, status, pageable);
        List<AdminUserDto> dtoList = users.getContent().stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, users.getTotalElements());
    }

    @Override
    @Transactional
    public void warnUser(Long userId, Long adminId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setWarningCount(user.getWarningCount() + 1);

        if (user.getWarningCount() % 3 == 0) {
            User admin = userRepository.findById(adminId).orElseThrow();
            user.setIsSuspended(true);
            user.setStatus(UserStatus.suspended);
            
            Suspended suspended = Suspended.builder()
                    .user(user)
                    .admin(admin)
                    .reason("Auto suspension due to 3 warnings")
                    .releasedAt(LocalDateTime.now().plusDays(1))
                    .build();
            suspendedRepository.save(suspended);
        }
    }

    @Override
    @Transactional
    public void suspendUser(Long userId, Long adminId, SuspendRequestDto requestDto) {
        User user = userRepository.findById(userId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        
        user.setIsSuspended(true);
        user.setStatus(UserStatus.suspended);
        
        Suspended suspended = Suspended.builder()
                .user(user)
                .admin(admin)
                .reason(requestDto.getReason())
                .releasedAt(requestDto.getReleasedAt())
                .build();
        suspendedRepository.save(suspended);
    }

    @Override
    @Transactional
    public void revertWarnUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getWarningCount() > 0) {
            boolean wasSuspendedByWarning = (user.getWarningCount() % 3 == 0) && user.getIsSuspended();
            user.setWarningCount(user.getWarningCount() - 1);
            
            if (wasSuspendedByWarning) {
                user.setIsSuspended(false);
                user.setStatus(UserStatus.active);
                suspendedRepository.findTopByUserIdOrderBySuspendedAtDesc(userId).ifPresent(suspended -> {
                    suspended.setReleasedAt(LocalDateTime.now());
                    suspended.setReason(reason);
                    suspendedRepository.save(suspended);
                });
            }
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void revertSuspendUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setIsSuspended(false);
        user.setStatus(UserStatus.active);
        userRepository.save(user);
        
        suspendedRepository.findTopByUserIdOrderBySuspendedAtDesc(userId).ifPresent(suspended -> {
            suspended.setReleasedAt(LocalDateTime.now());
            suspended.setReason(reason);
            suspendedRepository.save(suspended);
        });
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setStatus(UserStatus.valueOf(status.toLowerCase()));
        if (!status.equalsIgnoreCase("suspended")) {
            user.setIsSuspended(false);
        } else {
            user.setIsSuspended(true);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportAdminDto> getReports(String status, Pageable pageable) {
        List<Report> reports;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            reports = reportRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            reports = reportRepository.findAllByOrderByCreatedAtDesc();
        }
        // Applying pagination in memory for simplicity due to custom query return types
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reports.size());
        List<ReportAdminDto> dtoList = reports.subList(start, end).stream()
                .map(ReportAdminDto::new)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, reports.size());
    }

    @Override
    @Transactional
    public void updateReportStatus(Long reportId, String status) {
        reportRepository.updateStatus(reportId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReportTargetDetails(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        Map<String, Object> details = new HashMap<>();
        switch (report.getTargetType().name().toLowerCase()) {
            case "comment":
                Comment c = commentRepository.findById(report.getTargetId()).orElse(null);
                if (c != null) details.put("content", c.getContent());
                break;
            case "post":
                Post p = postRepository.findById(report.getTargetId()).orElse(null);
                if (p != null) {
                    details.put("title", p.getTitle());
                    details.put("content", p.getBody());
                }
                break;
        }
        return details;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.setStatus("deleted");
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void updateCommentStatus(Long commentId, String status) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.setStatus(status.toLowerCase());
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void hidePost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.setStatus("hidden");
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void updatePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.setStatus(status.toLowerCase());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void hideChannel(Long channelId) {
        Channel channel = channelRepository.findById(channelId).orElseThrow();
        channel.setStatus("hidden");
        channelRepository.save(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChannelDto> searchChannels(String keyword, String status, Pageable pageable) {
        String filterStatus = (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) ? status : null;
        Page<Channel> channels = channelRepository.searchChannels(keyword, filterStatus, pageable);
        
        List<AdminChannelDto> dtoList = channels.getContent().stream().map(channel -> {
            List<String> techStacks = channelTagRepository.findByChannel_Id(channel.getId()).stream()
                    .map(ct -> ct.getTag().getName())
                    .collect(Collectors.toList());
            return new AdminChannelDto(channel, techStacks);
        }).collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, channels.getTotalElements());
    }

    @Override
    @Transactional
    public void updateChannelStatus(Long channelId, String status) {
        Channel channel = channelRepository.findById(channelId).orElseThrow();
        channel.setStatus(status);
        channelRepository.save(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Suggestion> getSuggestions(Boolean isSeen, Pageable pageable) {
        List<Suggestion> suggestions;
        if (isSeen == null) {
            suggestions = suggestionRepository.findAllByOrderByIsSeenAscIdDesc();
        } else {
            suggestions = suggestionRepository.findByIsSeenOrderByIdDesc(isSeen);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), suggestions.size());
        return new PageImpl<>(suggestions.subList(start, end), pageable, suggestions.size());
    }

    @Override
    @Transactional
    public void markSuggestionAsSeen(Long suggestionId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        suggestion.setIsSeen(true);
        suggestionRepository.save(suggestion);
    }
}
