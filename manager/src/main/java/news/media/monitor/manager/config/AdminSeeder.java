package news.media.monitor.manager.config;

import news.media.monitor.manager.models.RoleName;
import news.media.monitor.manager.models.User;
import news.media.monitor.manager.repositories.RoleRepository;
import news.media.monitor.manager.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final String PROP_ADMIN_EMAIL    = "${app.admin.email:admin@example.com}";
    private static final String PROP_ADMIN_PASSWORD = "${app.admin.password:Admin123!}";
    private static final String PROP_ADMIN_NAME     = "${app.admin.name:Admin}";

    private static final String ERR_ROLE_USER_MISSING  = "ROLE_USER missing — check migrations";
    private static final String ERR_ROLE_ADMIN_MISSING = "ROLE_ADMIN missing — check migrations";
    private static final String LOG_ADMIN_SEEDED       = "Seeded default admin: {}";

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String          adminEmail;
    private final String          adminPassword;
    private final String          adminName;

    public AdminSeeder(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       @Value(PROP_ADMIN_EMAIL)    String adminEmail,
                       @Value(PROP_ADMIN_PASSWORD) String adminPassword,
                       @Value(PROP_ADMIN_NAME)     String adminName) {
        this.userRepository  = userRepository;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail      = adminEmail;
        this.adminPassword   = adminPassword;
        this.adminName       = adminName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        var userRole  = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(ERR_ROLE_USER_MISSING));
        var adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(ERR_ROLE_ADMIN_MISSING));

        var admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setName(adminName);
        admin.getRoles().add(userRole);
        admin.getRoles().add(adminRole);

        userRepository.save(admin);
        log.info(LOG_ADMIN_SEEDED, adminEmail);
    }
}