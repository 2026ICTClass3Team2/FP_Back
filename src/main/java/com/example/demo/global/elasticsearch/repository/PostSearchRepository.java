package com.example.demo.global.elasticsearch.repository;

import com.example.demo.global.elasticsearch.entity.PostSearchDoc;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PostSearchRepository extends ElasticsearchRepository<PostSearchDoc, String> {
    List<PostSearchDoc> findByTitleOrBody(String title, String body, Pageable pageable);
}

