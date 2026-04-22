package com.example.demo.domain.channel.repository;

import com.example.demo.domain.channel.entity.ChannelTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelTagRepository extends JpaRepository<ChannelTag, Long> {
    List<ChannelTag> findByChannel_Id(Long channelId);
    void deleteByChannel_Id(Long channelId);
}
