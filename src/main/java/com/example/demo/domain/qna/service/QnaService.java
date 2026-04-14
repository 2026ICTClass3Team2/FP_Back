package com.example.demo.domain.qna.service;

import com.example.demo.domain.content.entity.ContentTag;
import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.ContentTagRepository;
import com.example.demo.domain.content.repository.PostRepository;
import com.example.demo.domain.content.repository.TagRepository;
import com.example.demo.domain.qna.dto.QnaCardResponseDto;
import com.example.demo.domain.qna.dto.QnaCreateRequestDto;
import com.example.demo.domain.qna.dto.QnaDetailResponseDto;
import com.example.demo.domain.qna.entity.Qna;
import com.example.demo.domain.qna.repository.QnaRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final PostRepository postRepository;
    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ContentTagRepository contentTagRepository;

    @Transactional
    public void createQna(QnaCreateRequestDto qnaCreateRequestDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Post post = Post.builder()
                .title(qnaCreateRequestDto.getTitle())
                .body(qnaCreateRequestDto.getBody())
                .contentType("qna")
                .author(user)
                .authorName(user.getNickname())
                .build();
        Post savedPost = postRepository.save(post);

        Qna qna = Qna.builder()
                .post(savedPost)
                .rewardPoints(qnaCreateRequestDto.getRewardPoints())
                .build();
        qnaRepository.save(qna);

        List<String> tags = qnaCreateRequestDto.getTags();
        if (tags != null && !tags.isEmpty()) {
            for (String tagName : tags) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                ContentTag contentTag = ContentTag.builder()
                        .post(savedPost)
                        .tag(tag)
                        .build();
                contentTagRepository.save(contentTag);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<QnaCardResponseDto> getQnaList(String query, String sort, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return qnaRepository.findQnaList(query, sort, status, pageable);
    }

    @Transactional(readOnly = true)
    public QnaDetailResponseDto getQnaDetail(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
        Post post = qna.getPost();
        
        QnaDetailResponseDto dto = new QnaDetailResponseDto();
        dto.setQnaId(qna.getId());
        dto.setTitle(post.getTitle());
        dto.setBody(post.getBody());
        dto.setAuthor(post.getAuthor().getNickname());
        dto.setResolved(qna.isSolved());
        dto.setPoints(qna.getRewardPoints());
        dto.setImageUrl(post.getThumbnailUrl());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setCommentsCount(post.getCommentCount());
        dto.setLikes(post.getLikeCount());
        dto.setDislikes(post.getDislikeCount());
        dto.setViews(post.getViewCount());

        List<String> techStacks = post.getContentTags().stream()
                .map(contentTag -> contentTag.getTag().getName())
                .collect(Collectors.toList());
        dto.setTechStacks(techStacks);

        return dto;
    }
}
