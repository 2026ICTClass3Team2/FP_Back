package com.example.demo.global.elasticsearch.repository;

import com.example.demo.global.elasticsearch.document.UserSearchDoc;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserSearchRepository extends ElasticsearchRepository<UserSearchDoc, String> {
    List<UserSearchDoc> findByNicknameOrUsername(String nickname, String username, Pageable pageable);
}


