package app.demo.controllers;

import app.demo.dto.TopicDTO;
import app.demo.dto.UserDTO;
import app.demo.exceptions.TopicAlreadyExistsException;
import app.demo.services.AdminService;
import jakarta.transaction.Transactional;
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
}
