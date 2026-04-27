package com.example.demo.global.elasticsearch.dto;

import com.example.demo.global.elasticsearch.document.ChannelSearchDoc;
import com.example.demo.global.elasticsearch.document.PostSearchDoc;
import com.example.demo.global.elasticsearch.document.UserSearchDoc;
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


