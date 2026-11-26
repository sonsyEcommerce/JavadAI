package com.javadai.agent.memory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository
        extends JpaRepository<Memory, UUID> {

    Page<Memory> findAllByActiveTrueOrderByUpdatedAtDesc(
            Pageable pageable
    );

    Page<Memory> findAllByMemoryTypeAndActiveTrueOrderByUpdatedAtDesc(
            MemoryType memoryType,
            Pageable pageable
    );

    @Query(
        value = """
            SELECT m FROM Memory m
            WHERE m.active = true
            AND (
                LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY m.updatedAt DESC
            """,
        countQuery = """
            SELECT COUNT(m) FROM Memory m
            WHERE m.active = true
            AND (
                LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """
    )
    Page<Memory> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<Memory> findByIdAndActiveTrue(UUID id);
}