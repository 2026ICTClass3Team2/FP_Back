package com.example.demo.domain.notice.controller;

import com.example.demo.domain.content.entity.Post;
import com.example.demo.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NoticeController {

    private final NoticeRepository noticeRepository;

    // 1. 공지사항 목록 조회
    @GetMapping("/list")
    public List<Post> getAllNotices() {
        return noticeRepository.findAll();
    }

    // 2. PDF 파일을 직접 서버로 올릴 때 쓰는 메서드
    @PostMapping("/upload-pdf")
    public String uploadPDF(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return "파일이 없습니다.";

        try (InputStream is = file.getInputStream()) {
            PDDocument document = PDDocument.load(is);
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);
            document.close();

            Post notice = Post.builder()
                    .title(file.getOriginalFilename() + " (파싱됨)")
                    .body(extractedText)
                    .authorName("관리자")
                    .contentType("notice")
                    .status("active")
                    .viewCount(0)
                    .commentCount(0)
                    .dislikeCount(0)
                    .isHidden(false)
                    .isSolved(false)
                    .build();

            noticeRepository.save(notice);
            return "SUCCESS: PDF 파일 파싱 완료!";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    // 3. 리액트에서 데이터를 보낼 때 받는 메서드 (여기를 수정했습니다!)
    @PostMapping("/add")
    public String addNotice(@RequestBody Post notice) {
        try {
            // [추가] 제목이 이미 존재하면 저장하지 않고 종료 (중복 방지)
            if (noticeRepository.existsByTitle(notice.getTitle())) {
                System.out.println("중복된 공지사항 제외: " + notice.getTitle());
                return "SUCCESS: ALREADY_EXISTS";
            }

            // [안전장치] 이름 매칭 실패로 null이 들어왔을 때를 대비한 방어막
            if (notice.getContentType() == null) notice.setContentType("notice");
            if (notice.getSourceType() == null) notice.setSourceType("internal");
            if (notice.getViewCount() == null) notice.setViewCount(0);
            if (notice.getCommentCount() == null) notice.setCommentCount(0);
            if (notice.getLikeCount() == null) notice.setLikeCount(0);
            if (notice.getDislikeCount() == null) notice.setDislikeCount(0);
            if (notice.getIsHidden() == null) notice.setIsHidden(false);
            if (notice.getIsSolved() == null) notice.setIsSolved(false);

            noticeRepository.save(notice);
            System.out.println("공지사항 신규 저장 성공: " + notice.getTitle());
            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
}