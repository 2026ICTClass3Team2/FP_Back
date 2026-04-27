package com.example.demo.global.elasticsearch.dto;

import com.example.demo.global.elasticsearch.document.ChannelSearchDoc;
import com.example.demo.global.elasticsearch.document.PostSearchDoc;
import com.example.demo.global.elasticsearch.document.UserSearchDoc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class GlobalSearchResponse {
    private List<PostSearchDoc> posts;
    private List<UserSearchDoc> users;
    private List<ChannelSearchDoc> channels;
}

