package com.example.demo.domain.report.service;

import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.follow.repository.FollowRepository;
import com.example.demo.domain.report.dto.ReportRequestDto;
import com.example.demo.domain.report.entity.Block;
import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.enums.HiddenReasonType;
import com.example.demo.domain.report.enums.HiddenTargetType;
import com.example.demo.domain.report.enums.ReportStatus;
import com.example.demo.domain.report.enums.ReportTargetType;
import com.example.demo.domain.report.repository.BlockRepository;
import com.example.demo.domain.report.repository.HiddenRepository;
import com.example.demo.domain.report.repository.ReportRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String TARGET_TYPE_CHANNEL = "channel";

    private final ReportRepository reportRepository;
    private final BlockRepository blockRepository;
    private final HiddenRepository hiddenRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final ChannelRepository channelRepository;

    @Transactional
    public boolean processReport(ReportRequestDto requestDto, String reporterEmail) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new IllegalArgumentException("신고자를 찾을 수 없습니다."));
        // 1. 신고 접수
        Report report = Report.builder()
                .reporter(reporter)
                .targetId(requestDto.getTargetId())
                .targetType(requestDto.getTargetType())
                .category(requestDto.getReasonType())
                .details(requestDto.getReasonDetail())
                .status(ReportStatus.pending)
                .build();
        reportRepository.save(report);
        boolean isBlockedOrHidden = false;
        // 2. 게시글 신고 - 차단 여부에 관계없이 게시글 숨김
        if (requestDto.getTargetType() == ReportTargetType.post) {
            Post postToHide = postRepository.findById(requestDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("숨길 게시글을 찾을 수 없습니다."));
            if (!hiddenRepository.existsByUserIdAndTargetId(reporter.getId(), postToHide.getId())) {
                Hidden hidden = Hidden.builder()
                        .user(reporter)
                        .targetId(postToHide.getId())
                        .targetType(HiddenTargetType.valueOf(postToHide.getContentType()))
                        .reason(HiddenReasonType.reported)
                        .build();
                hiddenRepository.save(hidden);
            }
            isBlockedOrHidden = true;
        }

        // 3. 댓글 신고 - 신고한 댓글 숨김
        if (requestDto.getTargetType() == ReportTargetType.comments) {
            if (!hiddenRepository.existsByUserIdAndTargetId(reporter.getId(), requestDto.getTargetId())) {
                Hidden hidden = Hidden.builder()
                        .user(reporter)
                        .targetId(requestDto.getTargetId())
                        .targetType(HiddenTargetType.comment)
                        .reason(HiddenReasonType.reported)
                        .build();
                hiddenRepository.save(hidden);
            }
            isBlockedOrHidden = true;
        }
        // 4. 채널 차단 처리 (채널 신고 + blockChannel 체크 시)
        if (requestDto.getTargetType() == ReportTargetType.channel && requestDto.isBlockChannel()) {
            Long channelId = requestDto.getTargetId();
            if (!hiddenRepository.existsByUserIdAndTargetIdAndTargetType(reporter.getId(), channelId, HiddenTargetType.channel)) {
                Hidden hidden = Hidden.builder()
                        .user(reporter)
                        .targetId(channelId)
                        .targetType(HiddenTargetType.channel)
                        .reason(HiddenReasonType.reported)
                        .build();
                hiddenRepository.save(hidden);
            }
            // 구독 중이면 자동 구독 취소
            followRepository.findByUser_IdAndTargetIdAndTargetType(reporter.getId(), channelId, TARGET_TYPE_CHANNEL)
                    .ifPresent(follow -> {
                        followRepository.delete(follow);
                        channelRepository.updateFollowerCount(channelId, -1);
                    });
            isBlockedOrHidden = true;
        }

        // 5. 유저 차단 처리
        if (requestDto.isBlockUser()) {
            Long userIdToBlock = findUserIdToBlock(requestDto.getTargetType(), requestDto.getTargetId());
            User userToBlock = userRepository.findById(userIdToBlock)
                    .orElseThrow(() -> new IllegalArgumentException("차단할 사용자를 찾을 수 없습니다."));

            if (reporter.getId().equals(userToBlock.getId())) {
                throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
            }

            if (!blockRepository.existsByBlockerIdAndBlockedId(reporter.getId(), userToBlock.getId())) {
                Block block = Block.builder()
                        .blocker(reporter)
                        .blocked(userToBlock)
                        .build();
                blockRepository.save(block);
            }
            isBlockedOrHidden = true;
        }

        return isBlockedOrHidden;
    }

    private Long findUserIdToBlock(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case user:
                return targetId;
            case post:
                return postRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."))
                        .getAuthor()
                        .getId();
            case comments:
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
                if (comment.getAuthor() == null) {
                    throw new IllegalArgumentException("댓글 작성자를 찾을 수 없습니다.");
                }
                return comment.getAuthor().getId();
            default:
                throw new IllegalArgumentException("잘못된 신고 대상 타입입니다.");
        }
    }
}
