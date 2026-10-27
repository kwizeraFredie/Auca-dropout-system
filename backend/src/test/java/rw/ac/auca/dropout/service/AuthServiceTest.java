package rw.ac.auca.dropout.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.ac.auca.dropout.domain.Role;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.repository.SecurityEventRepository;
import rw.ac.auca.dropout.repository.RefreshTokenRepository;
import rw.ac.auca.dropout.repository.PasswordResetTokenRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityEventRepository securityEvents;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private PasswordResetTokenRepository passwordResetTokens;
    @Test
    void throttlesAfterFiveInvalidPasswords() {
        User user = user();
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);
        AuthService service = service();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.authenticate("student@example.com", "wrong"));
        }

        assertNotNull(user.getLoginLockedUntil());
        verify(users, times(5)).save(user);
    }

    @Test
    void successfulLoginClearsPreviousThrottleState() {
        User user = user();
        user.setFailedLoginAttempts(3);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", user.getPasswordHash())).thenReturn(true);
        AuthService service = service();

        service.authenticate("student@example.com", "correct");

        org.junit.jupiter.api.Assertions.assertEquals(0, user.getFailedLoginAttempts());
        verify(users).save(user);
    }

    private User user() {
        User user = new User();
        user.setRole(Role.STUDENT);
        user.setEmail("student@example.com");
        user.setPasswordHash("hash");
        return user;
    }

    private AuthService service() {
        return new AuthService(users, passwordEncoder, null, securityEvents,
            refreshTokens, passwordResetTokens, null);
    }
}
