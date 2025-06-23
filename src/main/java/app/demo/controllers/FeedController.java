package app.demo.controllers;

import app.demo.dto.ArticleDTO;
import app.demo.dto.TopicDTO;
import app.demo.services.FeedService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    private final FeedService feedService;

    @GetMapping("/topics")
    @Transactional
    ResponseEntity<List<TopicDTO>> getAllTopics() {
        try {
            List<TopicDTO> topics = feedService.getAllTopics();
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/articles")
    @Transactional
    ResponseEntity<List<ArticleDTO>> getAllArticles() {
        try {
            List<ArticleDTO> articles = feedService.getAllArticles();
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/articles/{topicName}")
    @Transactional
    ResponseEntity<List<ArticleDTO>> getArticlesByTopic( @PathVariable String topicName) {
        try {
            List<ArticleDTO> articles = feedService.getArticlesByTopic(topicName);
            return ResponseEntity.ok(articles);
        } catch (IllegalArgumentException e) {
            log.error("Topic not found: {}", topicName, e);
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            log.error("Error retrieving articles for topic: {}", topicName, e);
            return ResponseEntity.status(500).body(null);
        }
    }

}
