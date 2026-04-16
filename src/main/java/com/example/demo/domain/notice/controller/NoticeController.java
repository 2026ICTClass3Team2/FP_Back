package com.example.demo.domain.notice.controller;

import com.example.demo.domain.notice.dto.NoticeDto;
import com.example.demo.domain.notice.repository.repository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // ★ 중요: 리액트 포트(5173)의 접근을 허용함
public class NoticeController {

    private final repository noticeRepository;

    // Controller에 추가하면 리액트에서 목록을 바로 받아볼 수 있습니다.
    @GetMapping("/list")
    public List<NoticeDto> getAllNotices() {
        return noticeRepository.findAll();
    }

    @PostMapping("/upload-pdf")
    public String uploadPDF(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return "파일이 없습니다.";

        // try-with-resources 구문을 쓰면 작업 후 InputStream이 자동으로 닫힙니다.
        try (InputStream is = file.getInputStream()) {
            // 1. 메모리상에서 PDF 로드 (서버에 저장 안 함)
            PDDocument document = PDDocument.load(is);
            PDFTextStripper stripper = new PDFTextStripper();

            // 2. 텍스트 추출
            String extractedText = stripper.getText(document);
            document.close(); // 사용 직후 바로 닫기

            // 3. 엔티티에 담기 (PDF 파일은 여기서 버려짐)
            NoticeEntity notice = NoticeEntity.builder()
                    .title(file.getOriginalFilename() + " (파싱됨)")
                    .content(extractedText)
                    .build();

            // 4. DB 저장
            noticeRepository.save(notice);

            return "SUCCESS: PDF 데이터를 추출하여 DB에 저장했습니다.";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: PDF 처리 중 오류 발생";
        }
    }
}