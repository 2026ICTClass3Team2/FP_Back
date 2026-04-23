package com.example.demo.global.config;

import com.example.demo.domain.content.entity.Tag;
import com.example.demo.domain.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagSeeder implements CommandLineRunner {

    private final TagRepository tagRepository;

    @Override
    public void run(String... args) {
        // Your curated, perfect list of starting tags
        List<String> coreTags = List.of(
                "Java", "Spring Boot", "React", "Docker", "AWS",
                "Python", "Node.js", "MySQL", "JavaScript", "TypeScript",
                "Git", "Jenkins", "Kubernetes", "Linux", "HTML", "CSS", "Oracle",
                "Rust", "R", "Swift", "Vue.js"
        );

        int addedCount = 0;
        for (String tagName : coreTags) {
            // Only insert if it doesn't already exist
            if (tagRepository.findByName(tagName).isEmpty()) {
                tagRepository.save(Tag.builder().name(tagName).build());
                addedCount++;
            }
        }

        if (addedCount > 0) {
            log.info("Successfully seeded {} core tags into the database.", addedCount);
        }
    }
}