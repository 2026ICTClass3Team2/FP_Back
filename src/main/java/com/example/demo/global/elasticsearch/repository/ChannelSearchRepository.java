package com.example.demo.global.elasticsearch.repository;

import com.example.demo.global.elasticsearch.document.ChannelSearchDoc;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ChannelSearchRepository extends ElasticsearchRepository<ChannelSearchDoc, String> {
    List<ChannelSearchDoc> findByNameOrDescription(String name, String description, Pageable pageable);
}

