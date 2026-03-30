package org.example.filestorage.config;

import org.example.filestorage.repository.UserRepository;
import org.example.filestorage.service.AuthService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class SecurityTestConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return mock(AuthenticationManager.class);
    }

    @Bean
    public AuthService authService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        return new AuthService(userRepository, passwordEncoder, authenticationManager);
    }

}
