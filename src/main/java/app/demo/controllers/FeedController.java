package app.demo.controllers;

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
}
