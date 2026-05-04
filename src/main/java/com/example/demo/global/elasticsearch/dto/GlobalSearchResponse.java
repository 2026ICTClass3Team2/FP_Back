package com.example.demo.global.elasticsearch.dto;

import com.example.demo.global.elasticsearch.entity.ChannelSearchDoc;
import com.example.demo.global.elasticsearch.entity.PostSearchDoc;
import com.example.demo.global.elasticsearch.entity.UserSearchDoc;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {
    @Builder.Default
    private List<PostSearchDoc> posts = List.of();
    @Builder.Default
    private List<UserSearchDoc> users = List.of();
    @Builder.Default
    private List<ChannelSearchDoc> channels = List.of();
}


