package org.superwindcloud.shortlink.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.superwindcloud.shortlink.config.CacheConfig;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.repository.ShortLinkRepository;
import org.superwindcloud.shortlink.util.DistributedLock;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final Cache originalUrlCache;
    private final Cache shortCodeCache;
    private final DistributedLock distributedLock;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    private static final String LOCK_PREFIX = "shortlink:lock:";

    @Autowired
    public ShortLinkService(ShortLinkRepository shortLinkRepository, CacheManager cacheManager, DistributedLock distributedLock) {
        this.shortLinkRepository = shortLinkRepository;
        this.originalUrlCache = cacheManager.getCache(CacheConfig.ORIGINAL_URL_CACHE);
        this.shortCodeCache = cacheManager.getCache(CacheConfig.SHORT_CODE_CACHE);
        this.distributedLock = distributedLock;
    }

    /**
     * Creates a short link from the original URL
     */
    @Transactional
    public ShortLink createShortLink(String originalUrl) {
        String lockKey = LOCK_PREFIX + "url:" + originalUrl.hashCode();
        String lockValue = UUID.randomUUID().toString();
        
        // 尝试获取分布式锁
        if (!distributedLock.tryLock(lockKey, lockValue)) {
            // 如果无法获取锁，先检查是否已存在
            Optional<ShortLink> existingLink = findExistingShortLink(originalUrl);
            if (existingLink.isPresent()) {
                return existingLink.get();
            }
            // 如果仍然不存在，抛出异常或返回已存在的链接
            throw new IllegalStateException("Another process is creating a short link for this URL");
        }

        try {
            // 获取锁后再次检查是否已存在
            Optional<ShortLink> existingLink = findExistingShortLink(originalUrl);
            if (existingLink.isPresent()) {
                return existingLink.get();
            }

            for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
                String shortCode = generateShortCode();
                ShortLink shortLink = new ShortLink(originalUrl, shortCode);
                try {
                    ShortLink saved = shortLinkRepository.saveAndFlush(shortLink);
                    cacheShortLink(saved);
                    return saved;
                } catch (DataIntegrityViolationException ex) {
                    Optional<ShortLink> concurrentLink = findExistingShortLink(originalUrl);
                    if (concurrentLink.isPresent()) {
                        return concurrentLink.get();
                    }
                }
            }

            throw new IllegalStateException("Unable to generate unique short code after several attempts");
        } finally {
            // 释放分布式锁
            distributedLock.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * Finds a short link by its short code and increments the click count
     */
    public Optional<ShortLink> findAndIncrementClickCount(String shortCode) {
        Optional<ShortLink> existing = findByShortCode(shortCode);
        if (existing.isEmpty()) {
            return existing;
        }

        ShortLink link = existing.get();
        // 异步更新点击次数，避免阻塞响应
        updateClickCountAsync(link);
        return Optional.of(link);
    }

    /**
     * 异步更新点击次数
     */
    @Async
    public CompletableFuture<Void> updateClickCountAsync(ShortLink shortLink) {
        try {
            ShortLink link = new ShortLink(shortLink.getOriginalUrl(), shortLink.getShortCode());
            link.setId(shortLink.getId());
            link.setClickCount(shortLink.getClickCount() + 1);
            link.setCreatedAt(shortLink.getCreatedAt());
            
            ShortLink updated = shortLinkRepository.save(link);
            cacheShortLink(updated);
        } catch (Exception e) {
            // 记录错误但不抛出异常，以免影响主流程
            System.err.println("Error updating click count asynchronously: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Finds a short link by its short code without incrementing click count
     */
    public Optional<ShortLink> findByShortCode(String shortCode) {
        ShortLink cached = getFromCache(shortCodeCache, shortCode);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ShortLink> result = shortLinkRepository.findByShortCode(shortCode);
        result.ifPresent(this::cacheShortLink);
        return result;
    }

    /**
     * Generates a random short code
     */
    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder();
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            shortCode.append(CHARACTERS.charAt(ThreadLocalRandom.current().nextInt(CHARACTERS.length())));
        }

        return shortCode.toString();
    }

    private Optional<ShortLink> findExistingShortLink(String originalUrl) {
        ShortLink cached = getFromCache(originalUrlCache, originalUrl);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ShortLink> result = shortLinkRepository.findByOriginalUrl(originalUrl);
        result.ifPresent(this::cacheShortLink);
        return result;
    }

    private ShortLink getFromCache(Cache cache, String key) {
        if (cache == null || key == null) {
            return null;
        }
        return cache.get(key, ShortLink.class);
    }

    private void cacheShortLink(ShortLink shortLink) {
        if (shortLink == null) {
            return;
        }
        if (originalUrlCache != null) {
            originalUrlCache.put(shortLink.getOriginalUrl(), shortLink);
        }
        if (shortCodeCache != null) {
            shortCodeCache.put(shortLink.getShortCode(), shortLink);
        }
    }
}
