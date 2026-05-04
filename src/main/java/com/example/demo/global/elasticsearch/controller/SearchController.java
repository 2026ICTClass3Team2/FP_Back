package com.example.demo.global.elasticsearch.controller;

import com.example.demo.global.elasticsearch.dto.GlobalSearchResponse;
import com.example.demo.global.elasticsearch.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final GlobalSearchService searchService;

    @GetMapping
    public ResponseEntity<GlobalSearchResponse> globalSearch(@RequestParam String q) {
        return ResponseEntity.ok(searchService.searchEverything(q));
    }
}