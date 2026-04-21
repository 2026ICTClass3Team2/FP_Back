package com.example.demo.domain.channel.service;

import com.example.demo.domain.channel.dto.ChannelDetailDto;
import com.example.demo.domain.channel.dto.ChannelSummaryDto;
import com.example.demo.domain.channel.dto.SubscriberDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChannelService {
    Long createChannel(String channelName, String description, MultipartFile image, List<String> techStacks, String currentUsername);
    ChannelDetailDto getChannelDetail(Long channelId, String currentUsername);
    List<ChannelSummaryDto> getSubscribedChannels(String currentUsername);
    List<ChannelSummaryDto> getPopularChannels();
    void subscribeChannel(Long channelId, String currentUsername);
    void unsubscribeChannel(Long channelId, String currentUsername);
    List<SubscriberDto> getSubscribers(Long channelId);
}
