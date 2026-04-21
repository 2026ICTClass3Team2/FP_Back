package com.example.demo.domain.channel.service;

import com.example.demo.domain.channel.dto.ChannelDetailDto;
import com.example.demo.domain.channel.dto.ChannelSummaryDto;
import com.example.demo.domain.channel.dto.SubscriberDto;
import com.example.demo.domain.channel.entity.Channel;
import com.example.demo.domain.channel.entity.ChannelTag;
import com.example.demo.domain.channel.repository.ChannelRepository;
import com.example.demo.domain.channel.repository.ChannelTagRepository;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.follow.entity.Follow;
import com.example.demo.domain.follow.repository.FollowRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private static final String TARGET_TYPE_CHANNEL = "channel";

    private final ChannelRepository channelRepository;
    private final ChannelTagRepository channelTagRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    @Transactional
    public Long createChannel(String channelName, String description, MultipartFile image, List<String> techStacks, String currentUsername) {
        User owner = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = uploadImageToS3(image);
        }

        Channel channel = Channel.builder()
                .name(channelName)
                .description(description)
                .imageUrl(imageUrl)
                .owner(owner)
                .build();

        Channel savedChannel = channelRepository.save(channel);

        if (techStacks != null && !techStacks.isEmpty()) {
            for (String techName : techStacks) {
                Tag tag = tagRepository.findByName(techName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(techName).build()));

                ChannelTag channelTag = ChannelTag.builder()
                        .channel(savedChannel)
                        .tag(tag)
                        .build();
                channelTagRepository.save(channelTag);
            }
        }

        log.info("Channel created. channelId: {}, owner: {}", savedChannel.getId(), currentUsername);
        return savedChannel.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelDetailDto getChannelDetail(Long channelId, String currentUsername) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("채널을 찾을 수 없습니다."));

        List<String> techStacks = channelTagRepository.findByChannel_Id(channelId)
                .stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toList());

        User currentUser = currentUsername != null
                ? userRepository.findByEmail(currentUsername).orElse(null)
                : null;

        boolean isSubscribed = currentUser != null &&
                followRepository.existsByUser_IdAndTargetIdAndTargetType(currentUser.getId(), channelId, TARGET_TYPE_CHANNEL);

        return ChannelDetailDto.builder()
                .channelId(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .imageUrl(channel.getImageUrl())
                .followerCount(channel.getFollowerCount())
                .postCount(channel.getPostCount())
                .status(channel.getStatus())
                .techStacks(techStacks)
                .subscribed(isSubscribed)
                .ownerNickname(channel.getOwner() != null ? channel.getOwner().getNickname() : null)
                .ownerUsername(channel.getOwner() != null ? channel.getOwner().getUsername() : null)
                .ownerId(channel.getOwner() != null ? channel.getOwner().getId() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelSummaryDto> getSubscribedChannels(String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return followRepository.findByUser_IdAndTargetType(user.getId(), TARGET_TYPE_CHANNEL)
                .stream()
                .map(follow -> channelRepository.findById(follow.getTargetId()).orElse(null))
                .filter(ch -> ch != null && "active".equals(ch.getStatus()))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelSummaryDto> getPopularChannels() {
        return channelRepository.findTop5ByStatusOrderByFollowerCountDesc("active")
                .stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void subscribeChannel(Long channelId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("채널을 찾을 수 없습니다."));

        if (followRepository.existsByUser_IdAndTargetIdAndTargetType(user.getId(), channelId, TARGET_TYPE_CHANNEL)) {
            throw new IllegalStateException("이미 구독 중인 채널입니다.");
        }

        followRepository.save(Follow.builder()
                .user(user)
                .targetId(channelId)
                .targetType(TARGET_TYPE_CHANNEL)
                .build());

        channelRepository.updateFollowerCount(channelId, 1);
        log.info("Channel subscribed. channelId: {}, user: {}", channelId, currentUsername);
    }

    @Override
    @Transactional
    public void unsubscribeChannel(Long channelId, String currentUsername) {
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Follow follow = followRepository.findByUser_IdAndTargetIdAndTargetType(user.getId(), channelId, TARGET_TYPE_CHANNEL)
                .orElseThrow(() -> new IllegalStateException("구독 중인 채널이 아닙니다."));

        followRepository.delete(follow);
        channelRepository.updateFollowerCount(channelId, -1);
        log.info("Channel unsubscribed. channelId: {}, user: {}", channelId, currentUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriberDto> getSubscribers(Long channelId) {
        return followRepository.findByTargetIdAndTargetType(channelId, TARGET_TYPE_CHANNEL)
                .stream()
                .map(follow -> SubscriberDto.builder()
                        .userId(follow.getUser().getId())
                        .nickname(follow.getUser().getNickname())
                        .username(follow.getUser().getUsername())
                        .profilePicUrl(follow.getUser().getProfilePicUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private ChannelSummaryDto toSummaryDto(Channel channel) {
        return ChannelSummaryDto.builder()
                .channelId(channel.getId())
                .name(channel.getName())
                .imageUrl(channel.getImageUrl())
                .followerCount(channel.getFollowerCount())
                .build();
    }

    private String uploadImageToS3(MultipartFile image) {
        String uniqueFileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, uniqueFileName);
    }
}
