package com.example.demo.domain.content.service;

import com.example.demo.domain.algorithm.enums.FeedTab;
import com.example.demo.domain.algorithm.service.UserInterestService;
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
import com.example.demo.domain.follow.entity.Follow;
import com.example.demo.domain.follow.repository.FollowRepository;
import com.example.demo.domain.interaction.entity.Interaction;
import com.example.demo.domain.interaction.repository.InteractionRepository;
import com.example.demo.domain.notification.entity.NotificationTargetType;
import com.example.demo.domain.notification.service.NotificationService;
import com.example.demo.domain.report.entity.Hidden;
import com.example.demo.domain.report.enums.HiddenReasonType;
import com.example.demo.domain.report.enums.HiddenTargetType;
import com.example.demo.domain.report.repository.HiddenRepository;
import com.example.demo.domain.user.entity.Interest;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.InterestRepository;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final int ALGORITHM_CANDIDATE_LIMIT = 300;
    private static final int ALGORITHM_CANDIDATE_DAYS = 30;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ChannelRepository channelRepository;
    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;
    private final InteractionRepository interactionRepository;
    private final FollowRepository followRepository;
    private final InterestRepository interestRepository;
    private final HiddenRepository hiddenRepository;
    private final NotificationService notificationService;
    private final UserInterestService userInterestService;
    private final LlmTagService llmTagService;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public Long createPost(PostCreateRequestDto requestDto, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (requestDto.getChannelId() == null) {
            throw new IllegalArgumentException("채널을 선택해주세요.");
        }
        Channel channel = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

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
        channelRepository.updatePostCount(channel.getId(), 1);

        List<String> userTagNames = java.util.Collections.emptyList();
        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            userTagNames = requestDto.getTags();
            for (String tagName : userTagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

                ContentTag contentTag = ContentTag.builder()
                        .post(savedPost)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }

        llmTagService.assignTagsToPost(savedPost.getId(), savedPost.getTitle(), savedPost.getBody(),
                userTagNames, user.getId());

        // --- Notification Logic ---
        // 1. Channel Subscribers
        List<Follow> channelFollowers = followRepository.
                findByTargetIdAndTargetType(channel.getId(), "channel");
        for (Follow follow : channelFollowers) {
            User follower = follow.getUser();
            if (!follower.getId().equals(user.getId())) {
                String message = channel.getName() + " 채널에 새로운 게시글이 올라왔습니다.";
                notificationService.sendNotification(
                    follower, 
                    "new post", 
                    NotificationTargetType.channel, 
                    savedPost.getId(), 
                    message
                );
            }
        }

        // 2. User Followers
        List<Follow> userFollowers = followRepository.
                findByTargetIdAndTargetType(user.getId(), "user");
        for (Follow follow : userFollowers) {
            User follower = follow.getUser();
            String message = "팔로우하신 " + user.getNickname() + "님이 새로운 게시글을 올렸습니다.";
            notificationService.sendNotification(
                follower, 
                "new post", 
                NotificationTargetType.user, 
                savedPost.getId(), 
                message
            );
        }

        // 3. Mentions
        processMentions(savedPost, user);

        return savedPost.getId();
    }

    private void processMentions(Post post, User author) {
        if (post.getBody() == null) return;
        
        // Strip HTML tags for clean nickname extraction
        String plainContent = post.getBody().replaceAll("<[^>]*>", "");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([^\\s@]+)");
        java.util.regex.Matcher matcher = pattern.matcher(plainContent);
        java.util.Set<String> mentionedNicknames = new java.util.HashSet<>();
        
        while (matcher.find()) {
            mentionedNicknames.add(matcher.group(1));
        }

        for (String nickname : mentionedNicknames) {
            userRepository.findByNickname(nickname).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(author.getId())) {
                    String message = author.getNickname() + "님이 게시글에서 당신을 언급했습니다";
                    notificationService.sendNotification(
                        mentionedUser,
                        "mention",
                        NotificationTargetType.post,
                        post.getId(),
                        message
                    );
                }
            });
        }
    }

    @Override
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDto requestDto, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("이 게시물은 수정할 수 없는 상태입니다. (동결 또는 삭제)");
        }

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to modify this post");
        }

        post.setTitle(requestDto.getTitle());
        post.setBody(requestDto.getBody());

        String newThumbnailUrl = requestDto.getThumbnailUrl();
        if (newThumbnailUrl != null && !newThumbnailUrl.equals(post.getThumbnailUrl())) {
            deleteS3Object(post.getThumbnailUrl());
        }
        post.setThumbnailUrl(newThumbnailUrl);

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

        List<String> userTagNames = java.util.Collections.emptyList();
        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            userTagNames = requestDto.getTags();
            for (String tagName : userTagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

                ContentTag contentTag = ContentTag.builder()
                        .post(post)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }

        // 수정 시에도 LLM 태그 자동 추가 (작성자 태그 제외, 비동기) — userId null: 관심도 미반영
        llmTagService.assignTagsToPost(post.getId(), post.getTitle(), post.getBody(), userTagNames, null);

        log.info("Post updated. postId: {}, updatedBy: {}", postId, currentUsername);
        
        // Mentions on update
        processMentions(post, post.getAuthor());
    }

    // ─── 탭별 피드 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Object getFeedByTab(FeedTab tab, Long lastPostId, int page, int size, String currentUsername) {
        User currentUser = resolveUser(currentUsername);
        return switch (tab) {
            case LATEST -> getPostsFeed("LATEST", lastPostId, null, size, currentUsername);
            case POPULAR -> getPopularFeed(page, size, currentUser);
            case ALGORITHM -> getAlgorithmFeed(page, size, currentUser);
            case SUBSCRIBED -> getSubscribedFeed(page, size, currentUser);
        };
    }

    private Page<PostFeedResponseDto> getPopularFeed(int page, int size, User currentUser) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Long userId = currentUser != null ? currentUser.getId() : null;

        Page<Post> posts = (userId != null)
                ? postRepository.findPopularPosts(userId, pageRequest)
                : postRepository.findPopularPostsAnonymous(pageRequest);

        return posts.map(p -> convertToDto(p, currentUser));
    }

    private Page<PostFeedResponseDto> getAlgorithmFeed(int page, int size, User currentUser) {
        if (currentUser == null) {
            // 비로그인: 인기 피드로 대체
            return getPopularFeed(page, size, null);
        }

        List<Post> candidates = fetchAlgorithmCandidates(currentUser.getId(), null);
        Map<Long, Double> interestMap = buildInterestMap(currentUser.getId());
        List<PostFeedResponseDto> scored = scoreAndSort(candidates, interestMap, currentUser);
        return paginate(scored, page, size);
    }

    private Page<PostFeedResponseDto> getSubscribedFeed(int page, int size, User currentUser) {
        if (currentUser == null) {
            return Page.empty();
        }

        List<Long> channelIds = followRepository
                .findByUser_IdAndTargetType(currentUser.getId(), "channel")
                .stream().map(Follow::getTargetId).collect(Collectors.toList());

        if (channelIds.isEmpty()) {
            return Page.empty();
        }

        List<Post> candidates = fetchAlgorithmCandidates(currentUser.getId(), channelIds);
        Map<Long, Double> interestMap = buildInterestMap(currentUser.getId());
        List<PostFeedResponseDto> scored = scoreAndSort(candidates, interestMap, currentUser);
        return paginate(scored, page, size);
    }

    private List<Post> fetchAlgorithmCandidates(Long userId, List<Long> channelIds) {
        PageRequest limit = PageRequest.of(0, ALGORITHM_CANDIDATE_LIMIT);
        LocalDateTime since = LocalDateTime.now().minusDays(ALGORITHM_CANDIDATE_DAYS);
        return (channelIds == null)
                ? postRepository.findCandidatesForAlgorithm(userId, since, limit)
                : postRepository.findCandidatesForSubscribed(userId, channelIds, since, limit);
    }

    private Map<Long, Double> buildInterestMap(Long userId) {
        List<Interest> interests = interestRepository.findByUser_Id(userId);
        Map<Long, Double> map = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Interest i : interests) {
            long days = ChronoUnit.DAYS.between(i.getLastInteractionAt(), now);
            double decayed = i.getWeightScore() * Math.pow(0.99, days);
            map.put(i.getTag().getId(), decayed);
        }
        return map;
    }

    private List<PostFeedResponseDto> scoreAndSort(List<Post> posts, Map<Long, Double> interestMap, User user) {
        // 태그 일괄 로드 (N+1 방지)
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, List<ContentTag>> tagsByPost = contentTagRepository.findByPost_IdIn(postIds)
                .stream().collect(Collectors.groupingBy(ct -> ct.getPost().getId()));

        return posts.stream()
                .map(p -> {
                    double score = calcAlgorithmScore(p, tagsByPost.getOrDefault(p.getId(), List.of()), interestMap);
                    PostFeedResponseDto dto = convertToDto(p, user);
                    dto.setAlgorithmScore(score);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(PostFeedResponseDto::getAlgorithmScore).reversed())
                .collect(Collectors.toList());
    }

    private double calcAlgorithmScore(Post post, List<ContentTag> tags, Map<Long, Double> interestMap) {
        // 관심도 점수: 게시글 태그들의 평균 가중치
        double interestScore = 0.0;
        if (!tags.isEmpty()) {
            double tagSum = tags.stream()
                    .mapToDouble(ct -> interestMap.getOrDefault(ct.getTag().getId(), 0.0))
                    .sum();
            interestScore = tagSum / tags.size();
        }
        // 인기도 점수
        double rawPop = post.getLikeCount() * 2.0 + post.getCommentCount() * 3.0
                + post.getBookmarkCount() * 4.0 + post.getViewCount() * 0.1;
        double popularityScore = Math.min(rawPop / 100.0, 10.0);
        // 최신성 점수: 경과 시간이 길수록 감소
        double ageHours = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        double recencyScore = 1.0 / (1.0 + ageHours / 24.0);
        return interestScore * 0.4 + popularityScore * 0.3 + recencyScore * 0.3;
    }

    private Page<PostFeedResponseDto> paginate(List<PostFeedResponseDto> list, int page, int size) {
        int total = list.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new PageImpl<>(list.subList(from, to), PageRequest.of(page, size), total);
    }

    // ─── 관심없음 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void notInterested(Long postId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (hiddenRepository.existsByUserIdAndTargetId(user.getId(), postId)) {
            return; // 이미 숨김 처리
        }

        hiddenRepository.save(Hidden.builder()
                .user(user)
                .targetId(postId)
                .targetType(HiddenTargetType.feed)
                .reason(HiddenReasonType.not_interested)
                .build());

        userInterestService.onNotInterested(user.getId(), postId);
    }

    // ─── 공유 추적 ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void trackShare(Long postId, String currentUsername) {
        if (currentUsername == null) return;
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userInterestService.onShare(user.getId(), postId);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<PostFeedResponseDto> getPostsFeed(String tab, Long lastPostId, Integer page, int size, String currentUsername) {
        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }
        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;
        final User finalUser = currentUser;

        if ("POPULAR".equalsIgnoreCase(tab)) {
            PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size);
            return postRepository.findPopularPosts(currentUserId, pageRequest)
                    .map(post -> convertToDto(post, finalUser));
        }

        if ("ALGORITHM".equalsIgnoreCase(tab)) {
            PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size);
            List<Long> tagIds = java.util.Collections.emptyList();
            if (currentUser != null) {
                tagIds = interestRepository.findByUserId(currentUser.getId())
                        .stream()
                        .map(i -> i.getTag().getId())
                        .collect(Collectors.toList());
            }
            // tagIds가 비어있으면 매칭 수가 0으로 동일 → likeCount, createdAt 기준 정렬 (사실상 popular)
            // tagIds가 있으면 관심사 많이 매칭되는 게시물 우선
            if (tagIds.isEmpty()) {
                return postRepository.findPopularPosts(currentUserId, pageRequest)
                        .map(post -> convertToDto(post, finalUser));
            }
            return postRepository.findAlgorithmPosts(tagIds, currentUserId, pageRequest)
                    .map(post -> convertToDto(post, finalUser));
        }

        if ("SUBSCRIBED".equalsIgnoreCase(tab)) {
            if (currentUser == null) {
                return org.springframework.data.domain.Page.empty();
            }
            List<Long> channelIds = followRepository.findByUser_IdAndTargetType(currentUser.getId(), "channel")
                    .stream()
                    .map(f -> f.getTargetId())
                    .collect(Collectors.toList());
            if (channelIds.isEmpty()) {
                return org.springframework.data.domain.Page.empty();
            }
            PageRequest pageRequest = PageRequest.of(0, size);
            Slice<Post> posts;
            if (lastPostId == null) {
                posts = postRepository.findSubscribedPostsFirstPage(channelIds, currentUserId, pageRequest);
            } else {
                posts = postRepository.findSubscribedPostsCursor(channelIds, lastPostId, currentUserId, pageRequest);
            }
            return posts.map(post -> convertToDto(post, finalUser));
        }

        // LATEST (default)
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Post> posts;
        if (lastPostId == null) {
            posts = postRepository.findPostsFirstPage(currentUserId, pageRequest);
        } else {
            posts = postRepository.findPostsByCursor(lastPostId, currentUserId, pageRequest);
        }
        return posts.map(post -> convertToDto(post, finalUser));
    }

    @Override
    @Transactional
    public PostDetailResponseDto getPostDetail(Long postId, String currentUsername) {
        // Validate this is a feed post before incrementing
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!"feed".equals(post.getContentType()) ||
            (!"active".equals(post.getStatus()) && !"frozen".equals(post.getStatus()))) {
            throw new IllegalArgumentException("Feed post not found or hidden");
        }

        if (post.getChannel() != null && !"active".equals(post.getChannel().getStatus())) {
            throw new IllegalStateException("삭제된 채널입니다");
        }

        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }

        postRepository.increaseViewCount(postId);
        if (currentUser != null) {
            userInterestService.onView(currentUser.getId(), postId);
        }

        // re-fetch to get updated view count
        post = postRepository.findById(postId).get();

        return convertToDetailDto(post, currentUser);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String currentUsername) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("이 게시물은 삭제할 수 없는 상태입니다. (이미 삭제되었거나 동결됨)");
        }

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(currentUsername)) {
            throw new SecurityException("Unauthorized to delete this post");
        }

        post.setStatus("hidden");
        if (post.getChannel() != null) {
            channelRepository.updatePostCount(post.getChannel().getId(), -1);
        }
        log.info("Post hidden. postId: {}, hiddenBy: {}", postId, currentUsername);
    }

    @Override
    @Transactional
    public void toggleInteraction(Long postId, String actionType, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 게시물에는 상호작용할 수 없습니다.");
        }

        Optional<Interaction> existingInteraction = interactionRepository
                .findByUserIdAndTargetTypeAndTargetId(user.getId(), "post", post.getId());

        if (existingInteraction.isPresent()) {
            Interaction interaction = existingInteraction.get();
            if (interaction.getActionType().equals(actionType)) {
                interactionRepository.delete(interaction);
                updatePostInteractionCount(post, actionType, -1);
                // 취소 → 역방향 관심도 조정
                if ("like".equals(actionType)) userInterestService.onUnlike(user.getId(), postId);
                else if ("dislike".equals(actionType)) userInterestService.onUndislike(user.getId(), postId);
            } else {
                String previousActionType = interaction.getActionType();
                interaction.setActionType(actionType);
                updatePostInteractionCount(post, previousActionType, -1);
                updatePostInteractionCount(post, actionType, 1);
                // 전환: 단일 호출로 합산 delta 적용 (race condition 방지)
                if ("like".equals(previousActionType) && "dislike".equals(actionType)) {
                    userInterestService.onSwitchLikeToDislike(user.getId(), postId);
                } else if ("dislike".equals(previousActionType) && "like".equals(actionType)) {
                    userInterestService.onSwitchDislikeToLike(user.getId(), postId);
                }
            }
        } else {
            Interaction newInteraction = Interaction.builder()
                    .user(user)
                    .targetId(post.getId())
                    .targetType("post")
                    .actionType(actionType)
                    .build();
            interactionRepository.save(newInteraction);
            updatePostInteractionCount(post, actionType, 1);
            if ("like".equals(actionType)) userInterestService.onLike(user.getId(), postId);
            else if ("dislike".equals(actionType)) userInterestService.onDislike(user.getId(), postId);
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

        if (!"active".equals(post.getStatus())) {
            throw new IllegalArgumentException("동결되거나 삭제된 게시물은 북마크할 수 없습니다.");
        }

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndTargetIdAndTargetType(user.getId(), postId, post.getContentType());

        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            postRepository.updateBookmarkCount(postId, -1);
            userInterestService.onUnbookmark(user.getId(), postId);
            return false;
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .targetId(postId)
                    .targetType(post.getContentType())
                    .build();
            bookmarkRepository.save(bookmark);
            postRepository.updateBookmarkCount(postId, 1);
            userInterestService.onBookmark(user.getId(), postId);
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
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "post", post.getId());
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

    private PostDetailResponseDto convertToDetailDto(Post post, User currentUser) {
        boolean isAuthor = false;
        boolean isLiked = false;
        boolean isDisliked = false;
        boolean isBookmarked = false;

        if (currentUser != null) {
            isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
            Optional<Interaction> interaction = interactionRepository.findByUserIdAndTargetTypeAndTargetId(currentUser.getId(), "post", post.getId());
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
                .authorUserId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .authorNickname(post.getAuthor() != null ? post.getAuthor().getNickname() : post.getAuthorName())
                .authorProfileImageUrl(post.getAuthor() != null ? post.getAuthor().getProfilePicUrl() : null)
                .authorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .channelId(post.getChannel() != null ? post.getChannel().getId() : null)
                .channelName(post.getChannel() != null ? post.getChannel().getName() : null)
                .channelImageUrl(post.getChannel() != null ? post.getChannel().getImageUrl() : null)
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
    public Slice<PostFeedResponseDto> getChannelPosts(Long channelId, Long lastPostId, int page, int size, String sort, String currentUsername) {
        User currentUser = null;
        if (currentUsername != null) {
            currentUser = userRepository.findByEmail(currentUsername).orElse(null);
        }
        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;
        final User finalUser = currentUser;

        if ("POPULAR".equalsIgnoreCase(sort)) {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Post> posts = postRepository.findByChannelIdPopular(channelId, currentUserId, pageRequest);
            return posts.map(post -> convertToDto(post, finalUser));
        }

        // LATEST (default): cursor-based
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Post> posts;
        if (lastPostId == null) {
            posts = postRepository.findByChannelIdFirstPage(channelId, currentUserId, pageRequest);
        } else {
            posts = postRepository.findByChannelIdCursor(channelId, lastPostId, currentUserId, pageRequest);
        }
        return posts.map(post -> convertToDto(post, finalUser));
    }

    @Override
    @Transactional
    public void increaseViewCount(Long postId, Long userId) {
        postRepository.increaseViewCount(postId);
        if (userId != null) {
            userInterestService.onView(userId, postId);
        }
    }

    private User resolveUser(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    private void deleteS3Object(String url) {
        if (url == null || url.isBlank()) return;
        String key = url.substring(url.lastIndexOf('/') + 1);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}