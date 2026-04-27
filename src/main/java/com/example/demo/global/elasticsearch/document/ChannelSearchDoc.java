package com.example.demo.global.elasticsearch.document;

import com.example.demo.domain.channel.entity.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSearchDoc {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Integer)
    private Integer followerCount;

    public ChannelSearchDoc(Channel channel) {
        this.id = channel.getId().toString();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.imageUrl = channel.getImageUrl();
        this.followerCount = channel.getFollowerCount();
    }
}
