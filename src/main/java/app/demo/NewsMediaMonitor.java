package app.demo;

import app.demo.entities.Role;
import app.demo.entities.RoleName;
import app.demo.repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class NewsMediaMonitor {
	public static void main(String[] args) {
		SpringApplication.run(NewsMediaMonitor.class, args);
	}

	@Bean
	CommandLineRunner run(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.existsByAuthority(RoleName.ROLE_USER)) {
				roleRepository.save(new Role(RoleName.ROLE_USER));
			}
			if (roleRepository.existsByAuthority(RoleName.ROLE_ADMIN)) {
				roleRepository.save(new Role(RoleName.ROLE_ADMIN));
			}
		};
	}

}
