package app.demo.services;

import app.demo.dto.AuthDTO;
import app.demo.dto.LoginDTO;
import app.demo.dto.RegisterDTO;
import app.demo.entities.Role;
import app.demo.entities.RoleName;
import app.demo.entities.User;
import app.demo.exceptions.ExistingEmailException;
import app.demo.exceptions.ExistingUsernameException;
import app.demo.exceptions.ResourceNotFoundException;
import app.demo.mappers.UserMapper;
import app.demo.repositories.RoleRepository;
import app.demo.repositories.UserRepository;
import app.demo.utils.Constants;
import app.demo.security.JWTGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTGenerator jwtGenerator;

    @Transactional
    public void register(RegisterDTO registerDTO) throws ExistingUsernameException, ExistingEmailException, ResourceNotFoundException {

        if (userRepository.findByUsername(registerDTO.username()).isPresent()) {
            throw new ExistingUsernameException("Username '" + registerDTO.username() + "' already exists");
        }

        if (userRepository.findByEmail(registerDTO.email()).isPresent()) {
            throw new ExistingEmailException("Email '" + registerDTO.email() + "' already exists");
        }

        User user = userMapper.toEntity(registerDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        RoleName roleName = (registerDTO.adminRegisterCode() != null &&
                registerDTO.adminRegisterCode().equals(Constants.ADMIN_REGISTER_CODE))
                ? RoleName.ROLE_ADMIN
                : RoleName.ROLE_USER;

        // load role from database
        Role role = roleRepository.findByAuthority(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " not found"));

        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    @Transactional
    public AuthDTO login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.username(),
                        loginDTO.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtGenerator.generateToken(authentication);
        return new AuthDTO(jwt);
    }

}
