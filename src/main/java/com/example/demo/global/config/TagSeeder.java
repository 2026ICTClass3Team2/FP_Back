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
                "AWS", "Azure", "C#", "C++", "CSS", "Django", "Docker", "Figma", "Flask",
                "GCP", "Git", "Go", "GraphQL", "HTML", "Java", "JavaScript", "Jenkins", "Kotlin",
                "Kubernetes", "Linux", "MongoDB", "MySQL", "NestJS", "Next.js", "Nginx", "Node.js",
                "Oracle", "PHP", "PostgreSQL", "Python", "R", "React", "Redis", "Ruby", "Rust",
                "Spring Boot", "Svelte", "Swift", "Tailwind CSS", "TypeScript", "Vue.js", "Zustand"
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