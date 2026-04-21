package com.example.demo.domain.channel.entity;

import com.example.demo.domain.content.entity.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "channel_tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_channel_tag", columnNames = {"channel_id", "tag_id"})
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
