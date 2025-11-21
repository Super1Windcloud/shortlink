package org.superwindcloud.shortlink.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.superwindcloud.shortlink.entity.ShortLink;

@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
  Optional<ShortLink> findByShortCode(String shortCode);

  boolean existsByShortCode(String shortCode);

  boolean existsByOriginalUrl(String originalUrl);

  Optional<ShortLink> findByOriginalUrl(String originalUrl);
}
