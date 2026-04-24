package com.example.demo.domain.content.controller;

import com.example.demo.domain.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchTags(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "10") int size) {

        List<String> result = (keyword.isBlank()
                ? tagRepository.findAll()
                : tagRepository.findByNameContainingIgnoreCase(keyword))
                .stream()
                .map(tag -> tag.getName())
                .limit(size)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
