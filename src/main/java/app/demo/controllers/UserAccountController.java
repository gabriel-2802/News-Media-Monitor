package app.demo.controllers;

import app.demo.dto.TopicDTO;
import app.demo.dto.UserDTO;
import app.demo.exceptions.AlreadySubscribed;
import app.demo.exceptions.TopicNotFoundException;
import app.demo.security.JWTGenerator;
import app.demo.services.UserAccountService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Controller
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {
    private final UserAccountService userAccountService;

    @GetMapping("profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username, Authentication authentication) {

        if (!Objects.equals(username, authentication.getName())) {
            log.warn("Unauthorized access attempt by user: {}", authentication.getName());
            return ResponseEntity.status(403).body(null);
        }
        try {
            UserDTO userProfile = userAccountService.getUserProfile(username);
            return ResponseEntity.ok(userProfile);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving user profile for {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/subscribe/{topicId}")
    public ResponseEntity<TopicDTO> subscribeToTopic(@PathVariable Long topicId, Authentication authentication) {
        String username = authentication.getName();
        try {
            TopicDTO topicDTO = userAccountService.subscribeToTopic(topicId, username);
            return ResponseEntity.ok(topicDTO);
        } catch (UsernameNotFoundException | TopicNotFoundException e) {
            return ResponseEntity.status(404).body(null);
        } catch (AlreadySubscribed e) {
            return ResponseEntity.status(409).body(null);
        } catch (Exception e) {
            log.error("Error subscribing to topic {}: {}", topicId, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/unsubscribe/{topicId}")
    public ResponseEntity<Void> unsubscribeFromTopic(@PathVariable Long topicId, Authentication authentication) {
        String username = authentication.getName();
        try {
            userAccountService.unsubscribeFromTopic(topicId, username);
            return ResponseEntity.noContent().build();
        } catch (UsernameNotFoundException | TopicNotFoundException e) {
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            log.error("Error unsubscribing from topic {}: {}", topicId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
