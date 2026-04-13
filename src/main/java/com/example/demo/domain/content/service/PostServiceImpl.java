package com.example.demo.domain.content.service;

import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.content.dto.PostCreateRequestDto;
import com.example.demo.domain.content.dto.PostDetailResponseDto;
import com.example.demo.domain.content.dto.PostFeedResponseDto;
import com.example.demo.domain.content.dto.PostUpdateRequestDto;
import com.example.demo.domain.content.entity.Bookmark;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.repository.BookmarkRepository;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ChannelRepository channelRepository;
    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;
    private final InteractionRepository interactionRepository;
    // private final ViewHistoryRepository viewHistoryRepository; // NOTE: Schema validation 에러로 인해 임시 주석 처리

    @Override
    @Transactional
    public Long createPost(PostCreateRequestDto requestDto, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseGet(() -> userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")));

        Channel channel = null;
        if (requestDto.getChannelId() != null) {
            channel = channelRepository.findById(requestDto.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널입니다."));
        }

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .body(requestDto.getBody())
                .thumbnailUrl(requestDto.getThumbnailUrl())
                .contentType(requestDto.getContentType() != null ? requestDto.getContentType() : "feed")
                .author(user)
                .authorName(user.getNickname()) // 작성자 닉네임 저장
                .channel(channel)
                .sourceType("internal")
                .status("active")
                .build();

        Post savedPost = postRepository.save(post);

        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            for (String tagName : requestDto.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                
                ContentTag contentTag = ContentTag.builder()
                        .post(savedPost)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }

        return savedPost.getId();
    }

    @Override
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.getAuthor() == null || (!post.getAuthor().getUsername().equals(currentUsername) && !post.getAuthor().getEmail().equals(currentUsername))) {
            throw new SecurityException("수정 권한이 없습니다.");
        }

        post.setTitle(requestDto.getTitle());
        post.setBody(requestDto.getBody());
        if (requestDto.getThumbnailUrl() != null) {
            post.setThumbnailUrl(requestDto.getThumbnailUrl());
        }
        
        if (requestDto.getChannelId() != null) {
            Channel channel = channelRepository.findById(requestDto.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널입니다."));
            post.setChannel(channel);
        } else {
            post.setChannel(null);
        }

        contentTagRepository.deleteAllByPost(post);
        
        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            for (String tagName : requestDto.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                
                ContentTag contentTag = ContentTag.builder()
                        .post(post)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }
        
        log.info("Post updated. postId: {}, updatedBy: {}", postId, currentUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostFeedResponseDto> getPostsFeed(Long lastPostId, int size, String currentUsername) {
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Post> posts;

        if (lastPostId == null) {
            posts = postRepository.findPostsFirstPage(pageRequest);
        } else {
            posts = postRepository.findPostsByCursor(lastPostId, pageRequest);
        }

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername)
                    .orElseGet(() -> userRepository.findByUsername(currentUsername).orElse(null));
        }

        final User finalUser = currentUser;
        return posts.map(post -> convertToDto(post, finalUser));
    }

    @Override
    @Transactional
    public PostDetailResponseDto getPostDetail(Long postId, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername)
                    .orElseGet(() -> userRepository.findByUsername(currentUsername).orElse(null));
        }

        // NOTE: Schema validation 에러로 인해 임시 주석 처리
        // if (currentUser != null) {
        //     LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        //     boolean recentlyViewed = viewHistoryRepository.existsByUserIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
        //             currentUser.getId(), "posts", postId, oneHourAgo);
            
        //     if (!recentlyViewed) {
        //         post.setViewCount(post.getViewCount() + 1);
        //         ViewHistory viewHistory = ViewHistory.builder()
        //                 .user(currentUser)
        //                 .targetId(postId)
        //                 .targetType("posts")
        //                 .build();
        //         viewHistoryRepository.save(viewHistory);
        //     }
        // } else {
        //     post.setViewCount(post.getViewCount() + 1);
        // }
        post.setViewCount(post.getViewCount() + 1); // 조회수 증가 로직 단순화

        return convertToDetailDto(post, currentUser);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.getAuthor() == null || (!post.getAuthor().getUsername().equals(currentUsername) && !post.getAuthor().getEmail().equals(currentUsername))) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
        log.info("Post deleted. postId: {}, deletedBy: {}", postId, currentUsername);
    }

    @Override
    @Transactional
    public void toggleInteraction(Long postId, String actionType, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseGet(() -> userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "feed", post.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                interactionRepository.delete(interaction);
                updatePostInteractionCount(post, actionType, -1);
            } else {
                interactionRepository.delete(interaction);
                updatePostInteractionCount(post, interaction.getActionType(), -1);
                
                Interaction newInteraction = Interaction.builder()
                        .user(user)
                        .targetId(post.getId())
                        .targetType("feed")
                        .actionType(actionType)
                        .build();
                interactionRepository.save(newInteraction);
                updatePostInteractionCount(post, actionType, 1);
            }
        } else {
            Interaction newInteraction = Interaction.builder()
                    .user(user)
                    .targetId(post.getId())
                    .targetType("feed")
                    .actionType(actionType)
                    .build();
            interactionRepository.save(newInteraction);
            updatePostInteractionCount(post, actionType, 1);
        }
    }

    // 구버전/단일 토글 대응
    @Override
    @Transactional
    public boolean toggleLike(Long postId, String currentUsername) {
        toggleInteraction(postId, "like", currentUsername);
        return true;
    }

    @Override
    @Transactional
    public boolean toggleDislike(Long postId, String currentUsername) {
        toggleInteraction(postId, "dislike", currentUsername);
        return true;
    }

    private void updatePostInteractionCount(Post post, String actionType, int delta) {
        if ("like".equals(actionType)) {
            post.setLikeCount(Math.max(0, post.getLikeCount() + delta));
        } else if ("dislike".equals(actionType)) {
            post.setDislikeCount(Math.max(0, post.getDislikeCount() + delta));
        }
    }

    @Override
    @Transactional
    public boolean toggleBookmark(Long postId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseGet(() -> userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndTargetIdAndTargetType(user.getId(), postId, "feed");

        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            return false;
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .targetId(postId)
                    .targetType(post.getContentType())
                    .build();
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    private PostFeedResponseDto convertToDto(Post post, User currentUser) {
        boolean isAuthor = false;
        boolean isLiked = false;
        boolean isDisliked = false;
        boolean isBookmarked = false;

        if (currentUser != null) {
            isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
            Optional<Interaction> likeInteraction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "feed", post.getId());
            if (likeInteraction.isPresent()) {
                if ("like".equals(likeInteraction.get().getActionType())) {
                    isLiked = true;
                } else if ("dislike".equals(likeInteraction.get().getActionType())) {
                    isDisliked = true;
                }
            }
            
            isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), post.getContentType());
        }

        List<String> tags = new ArrayList<>();

        return PostFeedResponseDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .authorProfileImageUrl(post.getAuthor() != null ? post.getAuthor().getProfilePicUrl() : null)
                .authorNickname(post.getAuthor() != null ? post.getAuthor().getNickname() : post.getAuthorName())
                .authorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .channelName(post.getChannel() != null ? post.getChannel().getName() : null)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .shareCount(0)
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isBookmarked(isBookmarked)
                .isAuthor(isAuthor)
                .build();
    }

    private PostDetailResponseDto convertToDetailDto(Post post, User currentUser) {
        boolean isAuthor = false;
        boolean isLiked = false;
        boolean isDisliked = false;
        boolean isBookmarked = false;

        if (currentUser != null) {
            isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "feed", post.getId());
            if (interaction.isPresent()) {
                if ("like".equals(interaction.get().getActionType())) isLiked = true;
                if ("dislike".equals(interaction.get().getActionType())) isDisliked = true;
            }
            isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), post.getContentType());
        }

        List<String> tags = new ArrayList<>();

        return PostDetailResponseDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .thumbnailUrl(post.getThumbnailUrl())
                .contentType(post.getContentType())
                .authorNickname(post.getAuthor() != null ? post.getAuthor().getNickname() : post.getAuthorName())
                .authorProfileImageUrl(post.getAuthor() != null ? post.getAuthor().getProfilePicUrl() : null)
                .authorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .channelName(post.getChannel() != null ? post.getChannel().getName() : null)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .isBookmarked(isBookmarked)
                .isAuthor(isAuthor)
                .tags(tags)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
