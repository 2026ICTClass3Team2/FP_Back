package com.example.demo.domain.favorite.service;

import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.repository.BookmarkRepository;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.favorite.dto.FavoriteUserDto;
import com.example.demo.domain.follow.entity.Follow;
import com.example.demo.domain.follow.repository.FollowRepository;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final String TARGET_TYPE = "user";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final InteractionRepository interactionRepository;

    /** 즐겨찾기 토글 — true: 추가됨, false: 제거됨 */
    @Transactional
    public boolean toggleFavorite(String currentEmail, Long targetUserId) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 즐겨찾기할 수 없습니다.");
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        Optional<Follow> existing = followRepository
                .findByUser_IdAndTargetIdAndTargetType(currentUser.getId(), targetUserId, TARGET_TYPE);

        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            return false;
        } else {
            Follow follow = Follow.builder()
                    .user(currentUser)
                    .targetId(targetUserId)
                    .targetType(TARGET_TYPE)
                    .build();
            followRepository.save(follow);
            return true;
        }
    }

    /** 즐겨찾기한 유저 목록 */
    @Transactional(readOnly = true)
    public List<FavoriteUserDto> getFavoriteUsers(String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return followRepository.findByUser_IdAndTargetType(currentUser.getId(), TARGET_TYPE)
                .stream()
                .map(f -> userRepository.findById(f.getTargetId()).orElse(null))
                .filter(u -> u != null)
                .map(FavoriteUserDto::from)
                .collect(Collectors.toList());
    }

    /** 즐겨찾기 유저들의 게시글 피드 (커서 기반) */
    @Transactional(readOnly = true)
    public Slice<PostFeedResponseDto> getFavoritesFeed(String currentEmail, Long lastPostId, int size) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Long> authorIds = followRepository.findByUser_IdAndTargetType(currentUser.getId(), TARGET_TYPE)
                .stream()
                .map(Follow::getTargetId)
                .collect(Collectors.toList());

        if (authorIds.isEmpty()) {
            return new SliceImpl<>(java.util.Collections.emptyList());
        }

        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Post> postSlice = (lastPostId == null)
                ? postRepository.findFavoritesFeedFirstPage(authorIds, currentUser.getId(), pageRequest)
                : postRepository.findFavoritesFeedCursor(authorIds, lastPostId, currentUser.getId(), pageRequest);

        return postSlice.map(post -> convertToDto(post, currentUser));
    }

    public boolean isFavorited(Long currentUserId, Long targetUserId) {
        return followRepository.existsByUser_IdAndTargetIdAndTargetType(currentUserId, targetUserId, TARGET_TYPE);
    }

    private PostFeedResponseDto convertToDto(Post post, User currentUser) {
        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        boolean isLiked = false;
        boolean isDisliked = false;
        boolean isBookmarked = false;

        Optional<Interaction> interaction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "post", post.getId());
        if (interaction.isPresent()) {
            if ("like".equals(interaction.get().getActionType())) isLiked = true;
            if ("dislike".equals(interaction.get().getActionType())) isDisliked = true;
        }
        isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(
                currentUser.getId(), post.getId(), post.getContentType());

        List<String> tags = post.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toList());
        List<String> attachedUrls = post.getExternalUrl() != null && !post.getExternalUrl().isEmpty()
                ? List.of(post.getExternalUrl()) : List.of();

        return PostFeedResponseDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .thumbnailUrl(post.getThumbnailUrl())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .attachedUrls(attachedUrls)
                .authorUserId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .authorProfileImageUrl(post.getAuthor() != null ? post.getAuthor().getProfilePicUrl() : null)
                .authorNickname(post.getAuthor() != null ? post.getAuthor().getNickname() : post.getAuthorName())
                .authorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .channelId(post.getChannel() != null ? post.getChannel().getId() : null)
                .channelName(post.getChannel() != null ? post.getChannel().getName() : null)
                .channelImageUrl(post.getChannel() != null ? post.getChannel().getImageUrl() : null)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .bookmarkCount(post.getBookmarkCount())
                .shareCount(0)
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isBookmarked(isBookmarked)
                .isAuthor(isAuthor)
                .build();
    }
}
