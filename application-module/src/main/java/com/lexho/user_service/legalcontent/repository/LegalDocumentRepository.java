package com.lexho.user_service.legalcontent.repository;

import com.lexho.user_service.legalcontent.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    List<LegalDocument> findByPlatformAndActiveTrueOrderByUpdatedAtDesc(String platform);

    List<LegalDocument> findByDocumentTypeOrderByVersionDesc(String documentType);

    Optional<LegalDocument> findBySlugAndPlatformAndActiveTrue(String slug, String platform);

    Optional<LegalDocument> findTopBySlugAndPlatformAndActiveTrueOrderByVersionDesc(String slug, String platform);

    Optional<LegalDocument> findBySlugAndPlatformAndStatusAndActiveTrue(String slug, String platform, String status);

    Optional<LegalDocument> findTopByDocumentTypeAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
            String documentType,
            String status
    );

    Optional<LegalDocument> findTopBySlugAndPlatformAndStatusAndCurrentPublishedTrueAndActiveTrueOrderByVersionDesc(
            String slug,
            String platform,
            String status
    );

    Optional<LegalDocument> findTopByDocumentTypeOrderByVersionDesc(String documentType);

    @Query("select coalesce(max(d.version), 0) from LegalDocument d where d.documentType = :documentType")
    int maxVersion(@Param("documentType") String documentType);

    @Modifying
    @Query("""
            update LegalDocument d
               set d.currentPublished = false,
                   d.status = 'ARCHIVED',
                   d.archivedAt = current_timestamp
             where d.documentType = :documentType
               and d.status = 'PUBLISHED'
               and d.currentPublished = true
            """)
    int archiveCurrentPublished(@Param("documentType") String documentType);
}
