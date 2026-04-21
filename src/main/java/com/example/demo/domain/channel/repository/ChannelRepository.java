package com.example.demo.domain.channel.repository;

import com.example.demo.domain.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    @Query("SELECT c FROM Channel c LEFT JOIN c.owner o WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.id DESC")
    Page<Channel> searchChannels(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    List<Channel> findTop5ByStatusOrderByFollowerCountDesc(String status);

    @Modifying
    @Query("UPDATE Channel c SET c.followerCount = c.followerCount + :delta WHERE c.id = :channelId")
    void updateFollowerCount(@Param("channelId") Long channelId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Channel c SET c.postCount = c.postCount + :delta WHERE c.id = :channelId")
    void updatePostCount(@Param("channelId") Long channelId, @Param("delta") int delta);
}
