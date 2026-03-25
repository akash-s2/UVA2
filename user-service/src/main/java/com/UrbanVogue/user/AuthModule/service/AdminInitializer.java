package com.UrbanVogue.user.AuthModule.service;

import com.UrbanVogue.user.AuthModule.entity.User;
import com.UrbanVogue.user.AuthModule.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminInitializer {
    //  commandLinerunner we have and it will be the first file which will run when server start
    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository,
                                BCryptPasswordEncoder passwordEncoder) {

        return args -> {

            String adminEmail = "admin@urbanvogue.com";

            if (userRepository.findByEmail(adminEmail).isEmpty()) {

                User admin = new User();
                admin.setName("Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");

                userRepository.save(admin);

                System.out.println("Default Admin Created");
            }
        };
    }
}