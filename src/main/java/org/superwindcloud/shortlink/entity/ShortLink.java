package org.superwindcloud.shortlink.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "short_links",
    indexes = {
      @Index(name = "idx_short_links_short_code", columnList = "short_code", unique = true),
      @Index(name = "idx_short_links_original_url", columnList = "original_url", unique = true),
      @Index(name = "idx_short_links_created_at", columnList = "created_at")
    })
public class ShortLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 2048)
  @Column(name = "original_url", nullable = false, unique = true, length = 2048)
  private String originalUrl;

  @NotBlank
  @Column(name = "short_code", nullable = false, unique = true)
  private String shortCode;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "click_count")
  private Long clickCount = 0L;

  // Constructors
  public ShortLink() {}

  public ShortLink(String originalUrl, String shortCode) {
    this.originalUrl = originalUrl;
    this.shortCode = shortCode;
    this.createdAt = LocalDateTime.now();
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Long getClickCount() {
    return clickCount;
  }

  public void setClickCount(Long clickCount) {
    this.clickCount = clickCount;
  }

  // Increment click count method
  public void incrementClickCount() {
    this.clickCount++;
  }
}
