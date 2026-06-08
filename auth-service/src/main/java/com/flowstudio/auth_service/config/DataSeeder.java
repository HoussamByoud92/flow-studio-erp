package com.flowstudio.auth_service.config;

import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.entity.User;
import com.flowstudio.auth_service.repository.RoleRepository;
import com.flowstudio.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            List<String> roles = Arrays.asList("ADMIN", "HR", "PROJECT_LEAD", "CREATIVE", "CANDIDATE");

            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = Role.builder().name(roleName).build();
                    roleRepository.save(role);
                    log.info("Seeded role: {}", roleName);
                }
            }

            // Seed or update default admin user
            Role adminRole = roleRepository.findByName("ADMIN").get();
            User adminUser = userRepository.findByEmail("admin@flowstudio.ma").orElseGet(() -> 
                User.builder()
                    .username("admin")
                    .email("admin@flowstudio.ma")
                    .enabled(true)
                    .roles(java.util.Set.of(adminRole))
                    .build()
            );
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            userRepository.save(adminUser);
            log.info("Seeded/Updated default admin user: admin@flowstudio.ma / admin123");
        };
    }
}
