package app.demo.controllers;

import app.demo.dto.NewsSourceDTO;
import app.demo.dto.UserDTO;
import app.demo.exceptions.ExistingRssSource;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.services.AdminService;
import app.demo.services.monitoring.AbstractMonitorService;
import app.demo.services.monitoring.AsyncMonitorService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final AbstractMonitorService monitorService;

    @GetMapping("/users")
    @Transactional
    ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/users/{username}")
    @Transactional
    ResponseEntity<UserDTO> getUser(@PathVariable String username) {
        try {
            UserDTO user = adminService.getUser(username);
            return ResponseEntity.ok(user);
        } catch (UsernameNotFoundException e ) {
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/users/{username}")
    @Transactional
    ResponseEntity<String> deleteUser(@PathVariable String username) {
        try {
            adminService.deleteUser(username);
            return ResponseEntity.ok("User deleted successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting the user: " + e.getMessage());
        }
    }

    @PostMapping("topic/{topicName}")
    @Transactional
    ResponseEntity<String> createTopic(@PathVariable String topicName) {
        try {
            adminService.createTopic(topicName);
            return ResponseEntity.ok("Topic created successfully");
        } catch (TopicAlreadyExistsException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while creating the topic: " + e.getMessage());
        }
    }

    @DeleteMapping("/topic/{topicName}")
    @Transactional
    ResponseEntity<String> deleteTopic(@PathVariable String topicName) {
        try {
            adminService.deleteTopic(topicName);
            return ResponseEntity.ok("Topic deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting the topic: " + e.getMessage());
        }
    }

    @PostMapping("/news_source")
    ResponseEntity<NewsSourceDTO> createNewsSource(@Valid @RequestBody NewsSourceDTO newsSourceDTO) {
        try {
            NewsSourceDTO createdNewsSource = adminService.createNewsSource(newsSourceDTO);
            return ResponseEntity.ok(createdNewsSource);
        } catch (ExistingRssSource e) {
            return ResponseEntity.status(409).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/news_source/{id}")
    ResponseEntity<String> deleteNewsSource(@PathVariable String id) {
        try {
            String response = adminService.deleteNewsSource(Long.parseLong(id));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting the news source: " + e.getMessage());
        }
    }

    @GetMapping("/news_sources")
    ResponseEntity<List<NewsSourceDTO>> getNewsSources() {
        try {
            List<NewsSourceDTO> newsSources = adminService.getAllNewsSources();
            return ResponseEntity.ok(newsSources);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/monitor/start")
    public ResponseEntity<String> startMonitor() {
        try {
            monitorService.startMonitoring();
            return ResponseEntity.ok("Monitor started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while starting the monitor: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete_all_articles")
    public ResponseEntity<String> deleteAllArticles() {
        try {
            String response = adminService.deleteAllArticles();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting all articles: " + e.getMessage());
        }
    }


}
