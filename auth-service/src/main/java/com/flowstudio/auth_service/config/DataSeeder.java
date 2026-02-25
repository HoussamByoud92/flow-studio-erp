package com.flowstudio.auth_service.config;

import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        };
    }
}
