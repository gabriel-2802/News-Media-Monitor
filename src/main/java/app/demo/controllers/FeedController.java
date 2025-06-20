package app.demo.controllers;

import app.demo.dto.ArticleDTO;
import app.demo.dto.TopicDTO;
import app.demo.services.FeedService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
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
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/articles/{topicName}")
    @Transactional
    ResponseEntity<List<ArticleDTO>> getArticlesByTopic(String topicName) {
        try {
            List<ArticleDTO> articles = feedService.getArticlesByTopic(topicName);
            return ResponseEntity.ok(articles);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

}
