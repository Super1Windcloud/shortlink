package org.superwindcloud.shortlink.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.superwindcloud.shortlink.entity.ShortLink;
import org.superwindcloud.shortlink.exception.GlobalExceptionHandler;
import org.superwindcloud.shortlink.service.ShortLinkService;

@ExtendWith(MockitoExtension.class)
class ShortLinkControllerTest {

  @Mock private ShortLinkService shortLinkService;

  private MockMvc mockMvc;
  private LocalValidatorFactoryBean validator;

  @BeforeEach
  void setUp() {
    validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ShortLinkController(shortLinkService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void createShortLinkReturnsShortenedPayload() throws Exception {
    ShortLink shortLink = new ShortLink("https://example.com/article", "abc123");
    when(shortLinkService.createShortLink("https://example.com/article")).thenReturn(shortLink);

    mockMvc
        .perform(
            post("/api/shorten")
                .contentType(APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/article\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shortCode").value("abc123"))
        .andExpect(jsonPath("$.shortUrl").value("/r/abc123"))
        .andExpect(jsonPath("$.originalUrl").value("https://example.com/article"));
  }

  @Test
  void createShortLinkRejectsMalformedUrl() throws Exception {
    mockMvc
        .perform(
            post("/api/shorten").contentType(APPLICATION_JSON).content("{\"url\":\"not-a-url\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Invalid URL format"));
  }

  @Test
  void createShortLinkRejectsBlankUrl() throws Exception {
    mockMvc
        .perform(post("/api/shorten").contentType(APPLICATION_JSON).content("{\"url\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.fields.url").value("URL is required"));
  }

  @Test
  void redirectToOriginalUrlReturnsFound() throws Exception {
    ShortLink shortLink = new ShortLink("https://example.com/article", "abc123");
    shortLink.setId(1L);
    shortLink.setCreatedAt(LocalDateTime.now());
    when(shortLinkService.findAndIncrementClickCount("abc123")).thenReturn(Optional.of(shortLink));

    mockMvc
        .perform(get("/r/abc123"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://example.com/article"));
  }

  @Test
  void redirectToOriginalUrlReturnsNotFound() throws Exception {
    when(shortLinkService.findAndIncrementClickCount("missing")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/r/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Short link not found"));
  }
}
