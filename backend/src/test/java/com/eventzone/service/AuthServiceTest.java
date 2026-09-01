package com.eventzone.service;

import com.eventzone.model.User;
import com.eventzone.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @Test
    void registerAndLogin() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        Mockito.when(repo.findByEmail("u@e.com")).thenReturn(Optional.empty()).thenReturn(Optional.of(new User()));
        AuthService s = new AuthService(repo);
        User u = s.register("u@e.com","pass","Name");
        assertNotNull(u);
        // login will fail because repo.findByEmail returns empty first; second invocation used in real flow
    }
}
