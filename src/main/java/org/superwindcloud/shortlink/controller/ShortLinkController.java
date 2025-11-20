package org.superwindcloud.shortlink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.service.ShortLinkService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ShortLinkController {

    @Autowired
    private ShortLinkService shortLinkService;

    /**
     * Creates a short link from the original URL
     */
    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> createShortLink(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("url");
        
        if (originalUrl == null || originalUrl.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate URL format (basic validation)
        if (!isValidUrl(originalUrl)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid URL format");
            return ResponseEntity.badRequest().body(error);
        }

        ShortLink shortLink = shortLinkService.createShortLink(originalUrl);
        
        Map<String, String> response = new HashMap<>();
        response.put("shortUrl", "/r/" + shortLink.getShortCode());
        response.put("shortCode", shortLink.getShortCode());
        response.put("originalUrl", shortLink.getOriginalUrl());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Redirects to the original URL based on the short code
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortCode) {
        Optional<ShortLink> shortLink = shortLinkService.findAndIncrementClickCount(shortCode);
        
        if (shortLink.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", shortLink.get().getOriginalUrl())
                    .build();
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Short link not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Gets short link details by short code
     */
    @GetMapping("/info/{shortCode}")
    public ResponseEntity<?> getShortLinkInfo(@PathVariable String shortCode) {
        Optional<ShortLink> shortLink = shortLinkService.findByShortCode(shortCode);
        
        if (shortLink.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("shortCode", shortLink.get().getShortCode());
            response.put("originalUrl", shortLink.get().getOriginalUrl());
            response.put("clickCount", shortLink.get().getClickCount());
            response.put("createdAt", shortLink.get().getCreatedAt());
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Short link not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Basic URL validation (check if it starts with http or https)
     */
    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}