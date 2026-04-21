package com.example.demo.domain.channel.repository;

import com.example.demo.domain.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findTop5ByStatusOrderByFollowerCountDesc(String status);

    @Modifying
    @Query("UPDATE Channel c SET c.followerCount = c.followerCount + :delta WHERE c.id = :channelId")
    void updateFollowerCount(@Param("channelId") Long channelId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Channel c SET c.postCount = c.postCount + :delta WHERE c.id = :channelId")
    void updatePostCount(@Param("channelId") Long channelId, @Param("delta") int delta);
}
