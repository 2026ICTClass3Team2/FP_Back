package com.example.demo.global.elasticsearch.service;

import com.example.demo.global.elasticsearch.document.ChannelSearchDoc;
import com.example.demo.global.elasticsearch.document.PostSearchDoc;
import com.example.demo.global.elasticsearch.document.UserSearchDoc;
import com.example.demo.global.elasticsearch.dto.GlobalSearchResponse;
import com.example.demo.global.elasticsearch.repository.ChannelSearchRepository;
import com.example.demo.global.elasticsearch.repository.PostSearchRepository;
import com.example.demo.global.elasticsearch.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final PostSearchRepository postSearchRepo;
    private final UserSearchRepository userSearchRepo;
    private final ChannelSearchRepository channelSearchRepo;

    public GlobalSearchResponse searchEverything(String keyword) {
        // Fetch the top 5 matches for each category
        List<PostSearchDoc> matchedPosts = postSearchRepo.findByTitleOrBody(keyword, keyword, PageRequest.of(0, 5));
        List<UserSearchDoc> matchedUsers = userSearchRepo.findByNicknameOrUsername(keyword, keyword, PageRequest.of(0, 5));
        List<ChannelSearchDoc> matchedChannels = channelSearchRepo.findByNameOrDescription(keyword, keyword, PageRequest.of(0, 5));


        // Package them into the unified response
        return GlobalSearchResponse.builder()
                .posts(matchedPosts)
                .users(matchedUsers)
                .channels(matchedChannels)
                .build();
    }
}

