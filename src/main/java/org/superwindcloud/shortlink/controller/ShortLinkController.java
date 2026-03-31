package org.superwindcloud.shortlink.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.superwindcloud.shortlink.controller.dto.CreateShortLinkRequest;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.service.ShortLinkService;

@RestController
public class ShortLinkController {

  private final ShortLinkService shortLinkService;
  private static final int MAX_URL_LENGTH = 2048;

  public ShortLinkController(ShortLinkService shortLinkService) {
    this.shortLinkService = shortLinkService;
  }

  /** Creates a short link from the original URL */
  @PostMapping("/api/shorten")
  public ResponseEntity<Map<String, String>> createShortLink(
      @Valid @RequestBody CreateShortLinkRequest request) {
    String originalUrl = request.url().trim();

    if (!isValidUrl(originalUrl)) {
      return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL format"));
    }

    ShortLink shortLink = shortLinkService.createShortLink(originalUrl);

    return ResponseEntity.ok(
        Map.of(
            "shortUrl",
            "/r/" + shortLink.getShortCode(),
            "shortCode",
            shortLink.getShortCode(),
            "originalUrl",
            shortLink.getOriginalUrl()));
  }

  /** Redirects to the original URL based on the short code */
  @GetMapping("/r/{shortCode}")
  public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortCode) {
    Optional<ShortLink> shortLink = shortLinkService.findAndIncrementClickCount(shortCode);

    if (shortLink.isPresent()) {
      return ResponseEntity.status(HttpStatus.FOUND)
          .header("Location", shortLink.get().getOriginalUrl())
          .build();
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "Short link not found"));
  }

  /** Gets short link details by short code */
  @GetMapping("/api/info/{shortCode}")
  public ResponseEntity<?> getShortLinkInfo(@PathVariable String shortCode) {
    Optional<ShortLink> shortLink = shortLinkService.findByShortCode(shortCode);

    if (shortLink.isPresent()) {
      return ResponseEntity.ok(
          Map.of(
              "shortCode",
              shortLink.get().getShortCode(),
              "originalUrl",
              shortLink.get().getOriginalUrl(),
              "clickCount",
              shortLink.get().getClickCount(),
              "createdAt",
              shortLink.get().getCreatedAt()));
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "Short link not found"));
  }

  private boolean isValidUrl(String url) {
    if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
      return false;
    }
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          && uri.getHost() != null;
    } catch (URISyntaxException e) {
      return false;
    }
  }
}
