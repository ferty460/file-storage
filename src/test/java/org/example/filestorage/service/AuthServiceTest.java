package org.example.filestorage.service;

import org.assertj.core.api.Assertions;
import org.example.filestorage.AbstractIntegrationTest;
import org.example.filestorage.exception.UserAlreadyExistsException;
import org.example.filestorage.model.dto.request.AuthRequest;
import org.example.filestorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthServiceTest extends AbstractIntegrationTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void clearTable() {
        userRepository.deleteAll();
    }

    @Test
    void whenUserRegister_thenUserSavesInDb() {
        AuthRequest request = new AuthRequest("Leo", "leopass123");

        authService.register(request);

        assertEquals(1, userRepository.count());
        assertTrue(userRepository.existsByName("Leo"));
    }

    @Test
    void whenUserRegisterWithNonUniqueUsername_thenThrowsException() {
        AuthRequest leoRequest = new AuthRequest("Leo", "leopass123");
        authService.register(leoRequest);

        AuthRequest secLeoRequest = new AuthRequest("Leo", "secleopass123");

        Assertions.assertThatThrownBy(() -> authService.register(secLeoRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with name '%s' already exists".formatted(secLeoRequest.username()));
    }

}
