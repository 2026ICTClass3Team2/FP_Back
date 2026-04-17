package com.example.demo.domain.notice.repository;
import com.example.demo.domain.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Post, Long> {

    boolean existsByTitle(String title);
}