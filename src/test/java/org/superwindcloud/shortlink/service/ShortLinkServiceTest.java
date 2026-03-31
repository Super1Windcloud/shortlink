package org.superwindcloud.shortlink.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.superwindcloud.shortlink.config.CacheConfig;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.repository.ShortLinkRepository;
import org.superwindcloud.shortlink.util.DistributedLock;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

  @Mock private ShortLinkRepository shortLinkRepository;
  @Mock private CacheManager cacheManager;
  @Mock private Cache originalUrlCache;
  @Mock private Cache shortCodeCache;
  @Mock private DistributedLock distributedLock;

  private ShortLinkService shortLinkService;

  @BeforeEach
  void setUp() {
    when(cacheManager.getCache(CacheConfig.ORIGINAL_URL_CACHE)).thenReturn(originalUrlCache);
    when(cacheManager.getCache(CacheConfig.SHORT_CODE_CACHE)).thenReturn(shortCodeCache);
    shortLinkService = new ShortLinkService(shortLinkRepository, cacheManager, distributedLock);
  }

  @Test
  void createShortLinkReturnsExistingLinkWhenCacheHit() {
    ShortLink existing = new ShortLink("https://example.com/article", "abc123");
    when(distributedLock.tryLock(any(), any())).thenReturn(true);
    when(originalUrlCache.get("https://example.com/article", ShortLink.class)).thenReturn(existing);

    ShortLink result = shortLinkService.createShortLink("https://example.com/article");

    assertEquals(existing, result);
    verify(shortLinkRepository, never()).saveAndFlush(any());
    verify(distributedLock).releaseLock(any(), any());
  }

  @Test
  void findAndIncrementClickCountUsesAtomicRepositoryUpdateAndRefreshesCache() {
    ShortLink shortLink = new ShortLink("https://example.com/article", "abc123");
    shortLink.setId(7L);
    shortLink.setClickCount(5L);
    when(shortCodeCache.get("abc123", ShortLink.class)).thenReturn(shortLink);
    when(shortLinkRepository.incrementClickCountById(7L)).thenReturn(1);

    Optional<ShortLink> result = shortLinkService.findAndIncrementClickCount("abc123");

    assertTrue(result.isPresent());
    assertEquals(6L, result.get().getClickCount());
    verify(shortLinkRepository).incrementClickCountById(7L);
    verify(shortCodeCache).put("abc123", shortLink);
    verify(originalUrlCache).put("https://example.com/article", shortLink);
  }

  @Test
  void findAndIncrementClickCountSkipsUpdateWhenMissing() {
    when(shortCodeCache.get("missing", ShortLink.class)).thenReturn(null);
    when(shortLinkRepository.findByShortCode("missing")).thenReturn(Optional.empty());

    Optional<ShortLink> result = shortLinkService.findAndIncrementClickCount("missing");

    assertTrue(result.isEmpty());
    verify(shortLinkRepository, never()).incrementClickCountById(any());
  }

  @Test
  void createShortLinkReusesDatabaseRecordWhenLockBusy() {
    ShortLink existing = new ShortLink("https://example.com/article", "abc123");
    when(distributedLock.tryLock(any(), any())).thenReturn(false);
    when(originalUrlCache.get("https://example.com/article", ShortLink.class)).thenReturn(null);
    when(shortLinkRepository.findByOriginalUrl("https://example.com/article"))
        .thenReturn(Optional.of(existing));

    ShortLink result = shortLinkService.createShortLink("https://example.com/article");

    assertEquals(existing, result);
    verify(shortLinkRepository, never()).saveAndFlush(any());
    verify(originalUrlCache).put("https://example.com/article", existing);
    verify(shortCodeCache).put("abc123", existing);
  }
}
