package com.example.demo.global.elasticsearch.entity;

import com.example.demo.domain.channel.entity.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(replicas = 0, settingPath = "elasticsearch/settings.json")
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

    @Field(type = FieldType.Date,
            format = {DateFormat.date_hour_minute_second, DateFormat.date},
            pattern = "uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd")
    private LocalDateTime createdAt;

    public ChannelSearchDoc(Channel channel) {
        this.id = channel.getId().toString();
        this.name = channel.getName();
        this.description = channel.getDescription();
        this.imageUrl = channel.getImageUrl();
        this.followerCount = channel.getFollowerCount();
    }
}
