package com.example.demo.domain.report.service;

import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.report.dto.ReportRequestDto;
import com.example.demo.domain.report.entity.Block;
import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.entity.Report;
import com.example.demo.domain.report.enums.HiddenReasonType;
import com.example.demo.domain.report.enums.HiddenTargetType;
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

    private final ReportRepository reportRepository;
    private final BlockRepository blockRepository;
    private final HiddenRepository hiddenRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

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
                .build();
        reportRepository.save(report);

        boolean isBlockedOrHidden = false;

        // 2. 게시글 숨김 처리
        if (requestDto.isBlockPost() && requestDto.getTargetType() == ReportTargetType.post) {
            Post postToHide = postRepository.findById(requestDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("숨길 게시글을 찾을 수 없습니다."));

            Hidden hidden = Hidden.builder()
                    .user(reporter)
                    .targetId(postToHide.getId())
                    .targetType(HiddenTargetType.valueOf(postToHide.getContentType()))
                    .reason(HiddenReasonType.reported)
                    .build();
            hiddenRepository.save(hidden);
            isBlockedOrHidden = true;
        }

        // 3. 유저 차단 처리
        if (requestDto.isBlockUser()) {
            Long userIdToBlock = findUserIdToBlock(requestDto.getTargetType(), requestDto.getTargetId());
            User userToBlock = userRepository.findById(userIdToBlock)
                    .orElseThrow(() -> new IllegalArgumentException("차단할 사용자를 찾을 수 없습니다."));

            if (reporter.getId().equals(userToBlock.getId())) {
                throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
            }

            Block block = Block.builder()
                    .blocker(reporter)
                    .blocked(userToBlock)
                    .build();
            blockRepository.save(block);
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
                // TODO: CommentRepository 주입 후 구현 필요
                // 예: return commentRepository.findById(targetId).get().getUser().getId();
                throw new UnsupportedOperationException("댓글 작성자 차단은 아직 지원되지 않습니다.");
            default:
                throw new IllegalArgumentException("잘못된 신고 대상 타입입니다.");
        }
    }
}
