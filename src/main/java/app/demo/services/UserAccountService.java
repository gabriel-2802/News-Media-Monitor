package app.demo.services;

import app.demo.dto.UserDTO;
import app.demo.mappers.UserMapper;
import app.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDTO getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }


}
