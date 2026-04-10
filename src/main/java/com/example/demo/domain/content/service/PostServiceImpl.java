package com.example.demo.domain.content.service;

import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.content.dto.PostCreateRequestDto;
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

        // 태그 처리 로직
        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            for (String tagName : requestDto.getTags()) {
                // 기존 태그가 있으면 가져오고, 없으면 새로 생성
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

        // 수정 권한 검증 (작성자 본인인지 확인)
        if (post.getAuthor() == null || (!post.getAuthor().getUsername().equals(currentUsername) && !post.getAuthor().getEmail().equals(currentUsername))) {
            throw new SecurityException("수정 권한이 없습니다.");
        }

        // 기본 정보 업데이트
        post.setTitle(requestDto.getTitle());
        post.setBody(requestDto.getBody());
        if (requestDto.getThumbnailUrl() != null) {
            post.setThumbnailUrl(requestDto.getThumbnailUrl());
        }
        
        // 채널 변경 로직
        if (requestDto.getChannelId() != null) {
            Channel channel = channelRepository.findById(requestDto.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널입니다."));
            post.setChannel(channel);
        } else {
            post.setChannel(null);
        }

        // 태그 수정 로직 (기존 태그를 모두 지우고 새로 연결)
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
    public boolean toggleLike(Long postId, String currentUsername) {
        userRepository.findByEmail(currentUsername)
                .orElseGet(() -> userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")));

        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // likes 테이블이 없으므로 엔티티가 존재하지 않음. 로직 임시 삭제 및 변경 필요
        log.info("Toggle Like for postId: {}, user: {}", postId, currentUsername);
        
        return true; 
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
        boolean isBookmarked = false;

        if (currentUser != null) {
            isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
            // isLiked = likeRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), "post");
            isBookmarked = bookmarkRepository.existsByUserIdAndTargetIdAndTargetType(currentUser.getId(), post.getId(), post.getContentType());
        }

        // 태그 목록 가져오기
        List<String> tags = new ArrayList<>();
        // TODO: contentTag 연관관계를 통해 tag 이름을 가져오도록 추후 보완 (Fetch Join 등 고려)

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
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .shareCount(0)
                .isLiked(isLiked)
                .isBookmarked(isBookmarked)
                .isAuthor(isAuthor)
                .build();
    }
}
