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
}
