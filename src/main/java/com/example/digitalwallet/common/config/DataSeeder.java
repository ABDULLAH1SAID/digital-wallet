package com.example.digitalwallet.common.config;

import com.example.digitalwallet.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;

    @Value("${app.seed.enabled}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        if (userService.countUsers() > 0) {
            return;
        }

        userService.createUser("ahmed", "ahmed@example.com");
        userService.createUser("sara", "sara@example.com");
    }
}