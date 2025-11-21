package org.superwindcloud.shortlink.repository;

import org.superwindcloud.shortlink.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    boolean existsByOriginalUrl(String originalUrl);
    Optional<ShortLink> findByOriginalUrl(String originalUrl);
}