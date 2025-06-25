package app.demo.controllers;

import app.demo.dto.NewsSourceDTO;
import app.demo.dto.UserDTO;
import app.demo.exceptions.ExistingRssSource;
import app.demo.exceptions.SourceNotExisting;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.exceptions.TopicNotFoundException;
import app.demo.services.AdminService;
import app.demo.services.monitoring.MonitorService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final AdminService adminService;
    private final MonitorService monitorService;

    @GetMapping("/users")
    ResponseEntity<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/users/{username}")
    ResponseEntity<UserDTO> getUser(@PathVariable String username) {
        try {
            UserDTO user = adminService.getUser(username);
            return ResponseEntity.ok(user);
        } catch (UsernameNotFoundException e ) {
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            log.error("Error retrieving user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/users/{username}")
    ResponseEntity<String> deleteUser(@PathVariable String username) {
        try {
            adminService.deleteUser(username);
            return ResponseEntity.ok("User deleted successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("An error occurred while deleting the user " + username);
        }
    }

    @PostMapping("topic/{topicName}")
    ResponseEntity<String> createTopic(@PathVariable String topicName) {
        try {
            adminService.createTopic(topicName);
            return ResponseEntity.ok("Topic created successfully");
        } catch (TopicAlreadyExistsException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating topic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("An error occurred while creating the topic");
        }
    }

    @DeleteMapping("/topic/{topicName}")
    ResponseEntity<String> deleteTopic(@PathVariable String topicName) {
        try {
            adminService.deleteTopic(topicName);
            return ResponseEntity.ok("Topic deleted successfully");
        } catch (TopicNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting topic: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("An error occurred while deleting the topic " + topicName);
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
    ResponseEntity<NewsSourceDTO> deleteNewsSource(@PathVariable String id) {
        try {
            var response = adminService.deleteNewsSource(Long.parseLong(id));
            return ResponseEntity.ok(response);
        } catch (SourceNotExisting e) {
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            log.error("Error deleting news source: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/monitor/start")
    public ResponseEntity<String> startMonitor() {
        try {
            monitorService.startMonitoring();
            return ResponseEntity.ok("Monitor started successfully");
        } catch (Exception e) {
            log.error("Error starting monitor: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("An error occurred while starting the monitor: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete_all_articles")
    public ResponseEntity<String> deleteAllArticles() {
        try {
            adminService.deleteAllArticles();
            return ResponseEntity.ok("All articles deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting all articles: " + e.getMessage());
        }
    }


}
