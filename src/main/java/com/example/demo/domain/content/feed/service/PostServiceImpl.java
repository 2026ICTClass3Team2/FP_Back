package com.example.demo.domain.content.feed.service;

import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.content.feed.dto.PostCreateRequestDto;
import com.example.demo.domain.content.feed.dto.PostDetailResponseDto;
import com.example.demo.domain.content.feed.dto.PostFeedResponseDto;
import com.example.demo.domain.content.feed.dto.PostUpdateRequestDto;
import com.example.demo.domain.content.feed.entity.Bookmark;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.feed.entity.Tag;
import com.example.demo.domain.content.feed.entity.ContentTag;
import com.example.demo.domain.content.feed.repository.BookmarkRepository;
import com.example.demo.domain.content.feed.repository.ContentTagRepository;
import com.example.demo.domain.content.feed.repository.PostRepository;
import com.example.demo.domain.content.feed.repository.TagRepository;
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

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .body(requestDto.getBody())
                .thumbnailUrl(requestDto.getThumbnailUrl())
                .contentType(requestDto.getContentType() != null ? requestDto.getContentType() : "feed")
                .author(user)
                .authorName(user.getNickname())
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
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to modify this post");
        }

        post.setTitle(requestDto.getTitle());
        post.setBody(requestDto.getBody());
        if (requestDto.getThumbnailUrl() != null) {
            post.setThumbnailUrl(requestDto.getThumbnailUrl());
        }
        
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
        Slice<Post> posts;

        if (lastPostId == null) {
            posts = postRepository.findPostsFirstPage(pageRequest);
        } else {
            posts = postRepository.findPostsByCursor(lastPostId, pageRequest);
        }

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }

        final User finalUser = currentUser;
        return posts.map(post -> convertToDto(post, finalUser));
    }

    @Override
    @Transactional
    public PostDetailResponseDto getPostDetail(Long postId, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }

        post.setViewCount(post.getViewCount() + 1);

        return convertToDetailDto(post, currentUser);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to delete this post");
        }

        postRepository.delete(post);
        log.info("Post deleted. postId: {}, deletedBy: {}", postId, currentUsername);
    }

    @Override
    @Transactional
    public void toggleInteraction(Long postId, String actionType, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "feed", post.getId());

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
                    .targetType("feed")
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

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

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
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "feed", post.getId());
            if (interaction.isPresent()) {
                if ("like".equals(interaction.get().getActionType())) isLiked = true;
                if ("dislike".equals(interaction.get().getActionType())) isDisliked = true;
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
