package com.vivek.deskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// We authenticate with our own JWT filter, so Spring's default in-memory user is switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class DeskflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeskflowApplication.class, args);
    }
}
