package com.example.demo.domain.study.repository;

import com.example.demo.domain.study.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("SELECT r.id FROM Resource r WHERE r.isHidden = true")
    List<Long> findHiddenResourceIds();

    @Modifying
    @Query(value = "UPDATE resource SET is_hidden = :hidden WHERE resource_id = :id", nativeQuery = true)
    void updateResourceIsHidden(@Param("id") Long id, @Param("hidden") boolean hidden);

    @Modifying
    @Query(value = "UPDATE original SET is_hidden = :hidden WHERE resource_id = :id", nativeQuery = true)
    void updateOriginalIsHidden(@Param("id") Long id, @Param("hidden") boolean hidden);

    @Modifying
    @Query(value = "UPDATE translated t INNER JOIN original o ON t.original_id = o.original_id SET t.is_hidden = :hidden WHERE o.resource_id = :id", nativeQuery = true)
    void updateTranslatedIsHidden(@Param("id") Long id, @Param("hidden") boolean hidden);

    // ── 챕터(original) 관련 ──────────────────────────────────────────────────

    @Query(value = "SELECT original_id FROM original WHERE is_hidden = 1", nativeQuery = true)
    List<Long> findHiddenOriginalIds();

    @Modifying
    @Query(value = "UPDATE original SET is_hidden = 1 WHERE original_id = :id", nativeQuery = true)
    void hideOriginal(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE original SET is_hidden = 0 WHERE original_id = :id", nativeQuery = true)
    void restoreOriginal(@Param("id") Long id);

    // ── 상세 목록용 프로젝션 인터페이스 ─────────────────────────────────────

    interface HiddenLangRow {
        Long getResourceId();
        String getName();
        String getFirstChapterContent();
    }

    interface HiddenChapRow {
        Long getOriginalId();
        String getLanguageName();
        String getTitle();
        String getFirstContent();
    }

    // ── 삭제한 언어 상세 목록 (첫 번째 챕터 내용 앞 80자) ──────────────────

    @Query(value = """
            SELECT r.resource_id AS resourceId,
                   r.name        AS name,
                   LEFT(COALESCE(
                       (SELECT t.content
                        FROM translated t
                                 INNER JOIN original o ON t.original_id = o.original_id
                        WHERE o.resource_id = r.resource_id
                          AND o.is_hidden = 0
                          AND t.language = 'ko'
                        ORDER BY o.index_order ASC LIMIT 1),
                       (SELECT o.content
                        FROM original o
                        WHERE o.resource_id = r.resource_id
                          AND o.is_hidden = 0
                        ORDER BY o.index_order ASC LIMIT 1)
                   ), 80) AS firstChapterContent
            FROM resource r
            WHERE r.is_hidden = 1
            """, nativeQuery = true)
    List<HiddenLangRow> findHiddenLangsDetail();

    // ── 삭제한 챕터 상세 목록 (내용 앞 80자) ───────────────────────────────

    @Query(value = """
            SELECT o.original_id AS originalId,
                   r.name        AS languageName,
                   o.title       AS title,
                   LEFT(COALESCE(
                       (SELECT t.content FROM translated t
                        WHERE t.original_id = o.original_id AND t.language = 'ko'
                        LIMIT 1),
                       o.content
                   ), 80) AS firstContent
            FROM original o
                     INNER JOIN resource r ON r.resource_id = o.resource_id
            WHERE o.is_hidden = 1
            """, nativeQuery = true)
    List<HiddenChapRow> findHiddenChapsDetail();

    // ── 수정 ──────────────────────────────────────────────────────────────────

    @Modifying
    @Query(value = "UPDATE resource SET name = :name WHERE resource_id = :id", nativeQuery = true)
    void updateResourceName(@Param("id") Long id, @Param("name") String name);

    @Modifying
    @Query(value = "UPDATE translated SET title = :title, content = :content WHERE original_id = :id AND language = 'ko'", nativeQuery = true)
    int updateTranslatedChapter(@Param("id") Long id, @Param("title") String title, @Param("content") String content);

    @Modifying
    @Query(value = "UPDATE original SET title = :title, content = :content WHERE original_id = :id", nativeQuery = true)
    void updateOriginalChapter(@Param("id") Long id, @Param("title") String title, @Param("content") String content);
}
