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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public Long createPost(PostCreateRequestDto requestDto, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Channel channel = null;
        if (requestDto.getChannelId() != null) {
            channel = channelRepository.findById(requestDto.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
        }

        String externalUrl = null;
        if (requestDto.getAttachedUrls() != null && !requestDto.getAttachedUrls().isEmpty()) {
            externalUrl = requestDto.getAttachedUrls();
        }

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .body(requestDto.getBody())
                .thumbnailUrl(requestDto.getThumbnailUrl())
                .contentType("feed")
                .author(user)
                .authorName(user.getNickname())
                .channel(channel)
                .sourceType("internal")
                .status("active")
                .externalUrl(externalUrl)
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
        Post post = getFeedPost(postId);

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to modify this post");
        }

        post.setTitle(requestDto.getTitle());
        post.setBody(requestDto.getBody());
        post.setThumbnailUrl(requestDto.getThumbnailUrl());

        String externalUrl = null;
        if (requestDto.getAttachedUrls() != null && !requestDto.getAttachedUrls().isEmpty()) {
            externalUrl = requestDto.getAttachedUrls();
        }
        post.setExternalUrl(externalUrl);

        if (requestDto.getChannelId() != null) {
            Channel channel = channelRepository.findById(requestDto.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
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

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }
        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;

        Slice<Post> posts;
        if (lastPostId == null) {
            posts = postRepository.findPostsFirstPage(currentUserId, pageRequest);
        } else {
            posts = postRepository.findPostsByCursor(lastPostId, currentUserId, pageRequest);
        }

        final User finalUser = currentUser;
        return posts.map(post -> convertToDto(post, finalUser));
    }

    @Override
    @Transactional
    public PostDetailResponseDto getPostDetail(Long postId, String currentUsername) {
        // Validate this is a feed post before incrementing, then re-fetch so the
        // returned DTO reflects the already-incremented value.
        getFeedPost(postId); // validate type/status first (throws if not a valid feed post)
        postRepository.increaseViewCount(postId); // 조회수 증가 — DB updated
        Post post = getFeedPost(postId); // re-fetch: clearAutomatically=true ensures fresh value

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }

        return convertToDetailDto(post, currentUser);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String currentUsername) {
        Post post = getFeedPost(postId);

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to delete this post");
        }

        post.setStatus("hidden");
        log.info("Post hidden. postId: {}, hiddenBy: {}", postId, currentUsername);
    }

    @Override
    @Transactional
    public void toggleInteraction(Long postId, String actionType, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = getFeedPost(postId);

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), post.getContentType(), post.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                interactionRepository.delete(interaction);
                updatePostInteractionCount(post, actionType, -1);
            } else {
                String previousActionType = interaction.getActionType();
                interaction.setActionType(actionType);
                updatePostInteractionCount(post, previousActionType, -1);
                updatePostInteractionCount(post, actionType, 1);
            }
        } else {
            Interaction newInteraction = Interaction.builder()
                    .user(user)
                    .targetId(post.getId())
                    .targetType(post.getContentType())
                    .actionType(actionType)
                    .build();
            interactionRepository.save(newInteraction);
            updatePostInteractionCount(post, actionType, 1);
        }
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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = getFeedPost(postId);

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndTargetIdAndTargetType(user.getId(), postId, post.getContentType());

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
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), post.getContentType(), post.getId());
            if (interaction.isPresent()) {
                if ("like".equals(interaction.get().getActionType())) isLiked = true;
                if ("dislike".equals(interaction.get().getActionType())) isDisliked = true;
            }
            isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), post.getContentType());
        }

        List<String> tags = post.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toList());
        List<String> attachedUrls = new ArrayList<>();
        if (post.getExternalUrl() != null && !post.getExternalUrl().isEmpty()) {
            attachedUrls.add(post.getExternalUrl());
        }

        return PostFeedResponseDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .thumbnailUrl(post.getThumbnailUrl())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .attachedUrls(attachedUrls)
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
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), post.getContentType(), post.getId());
            if (interaction.isPresent()) {
                if ("like".equals(interaction.get().getActionType())) isLiked = true;
                if ("dislike".equals(interaction.get().getActionType())) isDisliked = true;
            }
            isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), post.getContentType());
        }

        List<String> tags = post.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toList());
        List<String> attachedUrls = new ArrayList<>();
        if (post.getExternalUrl() != null && !post.getExternalUrl().isEmpty()) {
            attachedUrls.add(post.getExternalUrl());
        }

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
                .attachedUrls(attachedUrls)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String getContentType(Long postId) {
        // 게시물을 ID로 조회하고, 없으면 예외.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        return post.getContentType();
    }

    private Post getFeedPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!"feed".equals(post.getContentType()) || !"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("Feed post not found");
        }

        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostFeedResponseDto> getChannelPosts(Long channelId, Long lastPostId, int size, String currentUsername) {
        PageRequest pageRequest = PageRequest.of(0, size);

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }

        Slice<Post> posts;
        if (lastPostId == null) {
            posts = postRepository.findByChannelIdFirstPage(channelId, pageRequest);
        } else {
            posts = postRepository.findByChannelIdCursor(channelId, lastPostId, pageRequest);
        }

        final User finalUser = currentUser;
        return posts.map(post -> convertToDto(post, finalUser));
    }

    //질문 게시판 조회수
    @Override
    @Transactional
    public void increaseViewCount(Long postId, Long userId) {
        // TODO: Implement view count logic, e.g., using a separate ViewHistory table to avoid incrementing on every refresh
        postRepository.increaseViewCount(postId);
    }
}