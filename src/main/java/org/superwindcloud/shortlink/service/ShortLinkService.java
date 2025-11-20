package org.superwindcloud.shortlink.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.repository.ShortLinkRepository;

import java.util.Optional;
import java.util.Random;

@Service
public class ShortLinkService {

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;

    /**
     * Creates a short link from the original URL
     */
    public ShortLink createShortLink(String originalUrl) {
        // Check if a short link for this URL already exists
        Optional<ShortLink> existingLink = findExistingShortLink(originalUrl);
        if (existingLink.isPresent()) {
            return existingLink.get();
        }

        String shortCode;
        do {
            shortCode = generateShortCode();
        } while (shortLinkRepository.existsByShortCode(shortCode));

        ShortLink shortLink = new ShortLink(originalUrl, shortCode);
        return shortLinkRepository.save(shortLink);
    }

    /**
     * Finds a short link by its short code and increments the click count
     */
    public Optional<ShortLink> findAndIncrementClickCount(String shortCode) {
        Optional<ShortLink> shortLink = shortLinkRepository.findByShortCode(shortCode);
        if (shortLink.isPresent()) {
            ShortLink link = shortLink.get();
            link.incrementClickCount();
            shortLinkRepository.save(link);
        }
        return shortLink;
    }

    /**
     * Finds a short link by its short code without incrementing click count
     */
    public Optional<ShortLink> findByShortCode(String shortCode) {
        return shortLinkRepository.findByShortCode(shortCode);
    }

    /**
     * Generates a random short code
     */
    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            shortCode.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return shortCode.toString();
    }

    /**
     * Checks if a short link for the given URL already exists
     */
    private Optional<ShortLink> findExistingShortLink(String originalUrl) {
        return shortLinkRepository.findByOriginalUrl(originalUrl);
    }
}