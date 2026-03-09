package org.example.filestorage.service;

import lombok.RequiredArgsConstructor;
import org.example.filestorage.model.User;
import org.example.filestorage.model.dto.request.LoginRequest;
import org.example.filestorage.model.dto.request.RegisterRequest;
import org.example.filestorage.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public void register(RegisterRequest request) {
        if (userRepository.existsByName(request.username())) {
            throw new RuntimeException("User already exists: " + request.username());
        }

        User user = new User();
        user.setName(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);
    }

    public void login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
