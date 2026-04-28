package com.example.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 실제 파일이 저장되는 물리적 경로 (프로젝트 루트의 upload-dir 폴더)
//        Path uploadDir = Paths.get("./upload-dir");
//        String uploadPath = uploadDir.toFile().getAbsolutePath();
//
//        // 브라우저에서 /api/admin/notice/download/파일명 으로 요청하면
//        // 실제 서버의 file:///.../upload-dir/파일명 을 찾아줌
//        registry.addResourceHandler("/admin/notice/download/**")
//                .addResourceLocations("file:" + uploadPath + "/");
//    }
}