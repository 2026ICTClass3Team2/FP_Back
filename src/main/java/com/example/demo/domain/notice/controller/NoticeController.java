package com.example.demo.domain.notice.controller;

import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.List;

//@RestController
//@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeRepository noticeRepository;

    // 1. 공지사항 목록 조회
    @GetMapping("/list")
    public List<Post> getAllNotices() {
        return noticeRepository.findAll();
    }

    // 2. 리액트에서 보낸 공지사항 저장 (CKEditor 마크다운/HTML 본문 그대로 저장)
    @PostMapping("/save")
    public String addNotice(@RequestBody Post notice) {
        try {
            if (notice.getTitle() == null || notice.getBody() == null) {
                return "ERROR: 필수 입력값이 없습니다.";
            }

            // 기본값 설정 (리액트에서 안 보냈을 경우 대비)
            if (notice.getContentType() == null) notice.setContentType("notice");
            if (notice.getStatus() == null) notice.setStatus("active");
            if (notice.getViewCount() == null) notice.setViewCount(0);

            noticeRepository.save(notice);
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @PostMapping("/increase-view/{id}")
    @Transactional // DB 수정을 위해 트랜잭션 유지
    public ResponseEntity<?> increaseView(@PathVariable Long id) {
        try {
            // 1. Repository에 새로 만든 쿼리 메서드 호출 (DB에서 직접 +1)
            int updatedRows = noticeRepository.incrementViewCount(id);

            if (updatedRows == 0) {
                return ResponseEntity.status(404).body("공지사항을 찾을 수 없습니다.");
            }

            // 2. 증가된 데이터를 DB에서 다시 읽어옴 (정확한 숫자를 위해)
            Post updatedNotice = noticeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("데이터 재조회 실패"));

            System.out.println("ID: " + id + " 조회수 증가 완료 -> " + updatedNotice.getViewCount());

            // 3. 리액트에서 필요한건 "증가된 숫자" 그 자체입니다.
            return ResponseEntity.ok(updatedNotice.getViewCount());

        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에 에러 출력
            return ResponseEntity.internalServerError().body("ERROR: " + e.getMessage());
        }
    }

    // 4. 공지사항 삭제
    @DeleteMapping("/delete/{id}")
    public String deleteNotice(@PathVariable Long id) {
        try {
            noticeRepository.deleteById(id);
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR";
        }
    }
}