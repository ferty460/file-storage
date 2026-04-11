package org.example.filestorage.service;

import org.example.filestorage.config.SecurityTestConfig;
import org.example.filestorage.exception.UserAlreadyExistsException;
import org.example.filestorage.model.User;
import org.example.filestorage.model.dto.request.AuthRequest;
import org.example.filestorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecurityTestConfig.class)
public class AuthServiceTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @MockitoBean
    private RedisConnectionFactory factory;

    @MockitoBean
    private RedisTemplate<?, ?> template;

    @MockitoBean
    private RedisIndexedSessionRepository repository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private MinioStorageService storageService;

    @BeforeAll
    static void init() {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
    }

    @Test
    void register_ShouldCreateNewUserInDatabase() {
        AuthRequest request = new AuthRequest("testuser", "password123");

        authService.register(request);

        Optional<User> savedUser = userRepository.findByName("testuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("testuser");
        assertThat(savedUser.get().getPassword()).isNotNull();
        assertThat(savedUser.get().getId()).isNotNull();
    }

    @Test
    void register_WithDuplicateUsername_ShouldThrowUserAlreadyExistsException() {
        AuthRequest request = new AuthRequest("duplicateUser", "password123");
        authService.register(request);

        AuthRequest duplicateRequest = new AuthRequest("duplicateUser", "anotherPassword");
        assertThatThrownBy(() -> authService.register(duplicateRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with name 'duplicateUser' already exists");

        long count = userRepository.findAll().stream()
                .filter(u -> u.getName().equals("duplicateUser"))
                .count();
        assertThat(count).isEqualTo(1);
    }

}
