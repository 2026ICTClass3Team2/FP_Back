package com.example.demo.global.elasticsearch.service;

import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.global.elasticsearch.dto.GlobalSearchResponse;
import com.example.demo.global.elasticsearch.dto.TagResult;
import com.example.demo.global.elasticsearch.entity.ChannelSearchDoc;
import com.example.demo.global.elasticsearch.entity.PostSearchDoc;
import com.example.demo.global.elasticsearch.entity.UserSearchDoc;
import com.example.demo.global.elasticsearch.repository.ChannelSearchRepository;
import com.example.demo.global.elasticsearch.repository.PostSearchRepository;
import com.example.demo.global.elasticsearch.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final PostSearchRepository postSearchRepo;
    private final UserSearchRepository userSearchRepo;
    private final ChannelSearchRepository channelSearchRepo;
    private final TagRepository tagRepository;

    public GlobalSearchResponse searchEverything(String keyword) {
        return searchEverything(keyword, 5);
    }

    public GlobalSearchResponse searchEverything(String keyword, int size) {
        List<PostSearchDoc> matchedPosts = postSearchRepo.findByTitleOrBody(keyword, keyword, PageRequest.of(0, size));
        List<UserSearchDoc> matchedUsers = userSearchRepo.findByNicknameOrUsername(keyword, keyword, PageRequest.of(0, size));
        List<ChannelSearchDoc> matchedChannels = channelSearchRepo.findByNameOrDescription(keyword, keyword, PageRequest.of(0, size));
        List<TagResult> matchedTags = tagRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .limit(size)
                .map(t -> new TagResult(t.getId(), t.getName()))
                .collect(Collectors.toList());

        return GlobalSearchResponse.builder()
                .posts(matchedPosts)
                .users(matchedUsers)
                .channels(matchedChannels)
                .tags(matchedTags)
                .build();
    }
}

