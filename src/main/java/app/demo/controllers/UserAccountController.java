package app.demo.controllers;

import app.demo.dto.UserDTO;
import app.demo.services.UserAccountService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {
    private final UserAccountService userAccountService;

    @GetMapping("profile/{username}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) {
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

}
