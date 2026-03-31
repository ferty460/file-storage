package org.example.filestorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filestorage.exception.InvalidCredentialsException;
import org.example.filestorage.exception.UserAlreadyExistsException;
import org.example.filestorage.model.User;
import org.example.filestorage.model.dto.request.AuthRequest;
import org.example.filestorage.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public void register(AuthRequest request) {
        if (userRepository.existsByName(request.username())) {
            log.warn("Registration failed - user already exists: {}", request.username());
            throw new UserAlreadyExistsException("User with name '%s' already exists".formatted(request.username()));
        }

        User user = new User();
        user.setName(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);
        log.info("User registered successfully: {}", request.username());
    }

    public void login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("User logged in successfully: {}", request.username());
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.username());
            throw new InvalidCredentialsException("Wrong username or password");
        }
    }

    public void logout() {
        log.info("User logged out");
        SecurityContextHolder.clearContext();
    }

}
