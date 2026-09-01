package com.eventzone.service;

import com.eventzone.model.User;
import com.eventzone.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, UUID> tokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String email, String password, String name) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setName(name);
        return userRepository.save(u);
    }

    public Optional<String> login(String email, String password) {
        Optional<User> o = userRepository.findByEmail(email);
        if (o.isPresent()) {
            User u = o.get();
            if (passwordEncoder.matches(password, u.getPasswordHash())) {
                String token = UUID.randomUUID().toString();
                tokens.put(token, u.getId());
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByToken(String token) {
        UUID uid = tokens.get(token);
        if (uid == null) return Optional.empty();
        return userRepository.findById(uid);
    }
}
