package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.UserJoinDTO;
import com.example.demo.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/member")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> join(@RequestBody UserJoinDTO userJoinDTO) {
        log.info("join: {}", userJoinDTO);

        userService.join(userJoinDTO);

        return ResponseEntity.ok(Map.of("result", "success"));
    }
}