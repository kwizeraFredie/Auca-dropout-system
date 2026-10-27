package rw.ac.auca.dropout.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.Role;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.repository.SecurityEventRepository;
import rw.ac.auca.dropout.repository.RefreshTokenRepository;
import rw.ac.auca.dropout.repository.PasswordResetTokenRepository;
import rw.ac.auca.dropout.domain.SecurityEvent;
import rw.ac.auca.dropout.domain.RefreshToken;
import rw.ac.auca.dropout.domain.PasswordResetToken;
import rw.ac.auca.dropout.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.security.SecureRandom;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityEventRepository securityEvents;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordResetEmailService passwordResetEmail;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       SecurityEventRepository securityEvents, RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository passwordResetTokens,
                       PasswordResetEmailService passwordResetEmail) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityEvents = securityEvents;
        this.refreshTokens = refreshTokens;
        this.passwordResetTokens = passwordResetTokens;
        this.passwordResetEmail = passwordResetEmail;
    }

    @Transactional
    public User registerStudent(String email, String password, String firstName, String lastName,
                                String gender, String studentNumber, rw.ac.auca.dropout.domain.Department department) {
        if (department == null) {
            throw new IllegalArgumentException("A department is required for student registration");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account already exists for this email");
        }
        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setGender(gender);
        user.setStudentNumber(studentNumber);
        user.setDepartment(department);
        user.setRole(Role.STUDENT);
        user.setAccountLocked(false);
        return users.save(user);
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class, IllegalStateException.class})
    public User authenticate(String email, String password) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (user.isAccountLocked()) {
            recordEvent("LOGIN_REJECTED_ACCOUNT_LOCKED", user);
            log.warn("Rejected login for academically locked account: {}", user.getId());
            throw new IllegalStateException("This student account is locked pending intervention");
        }
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(Instant.now())) {
            recordEvent("LOGIN_REJECTED_THROTTLED", user);
            log.warn("Rejected login during throttle window: {}", user.getId());
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setLoginLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
                log.warn("Login throttle activated for account: {}", user.getId());
            }
            users.save(user);
            recordEvent("LOGIN_FAILED", user);
            throw new IllegalArgumentException("Invalid email or password");
        }
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        users.save(user);
        recordEvent("LOGIN_SUCCEEDED", user);
        log.info("Successful login for account: {}", user.getId());
        return user;
    }

    private void recordEvent(String type, User user) {
        SecurityEvent event = new SecurityEvent();
        event.setEventType(type);
        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        securityEvents.save(event);
    }

    @Transactional
    public AuthTokens issueTokens(User user) {
        String refreshToken = randomToken();
        RefreshToken stored = new RefreshToken();
        stored.setUserId(user.getId());
        stored.setTokenHash(hash(refreshToken));
        stored.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        refreshTokens.save(stored);
        return new AuthTokens(jwtService.issue(user), refreshToken, user);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(Instant.now())) {
            if (current.getRevokedAt() != null) refreshTokens.revokeAllByUserId(current.getUserId());
            throw new IllegalArgumentException("Invalid refresh token");
        }
        User user = users.findById(current.getUserId())
                .filter(account -> !account.isAccountLocked())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        current.setRevokedAt(Instant.now());
        refreshTokens.save(current);
        return issueTokens(user);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokens.save(token);
        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = randomToken();
            passwordResetTokens.revokeActiveByUserId(user.getId());
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
            passwordResetTokens.save(token);
            passwordResetEmail.send(user.getEmail(), rawToken);
            recordEvent("PASSWORD_RESET_REQUESTED", user);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        token.setUsedAt(Instant.now());
        users.save(user);
        passwordResetTokens.save(token);
        refreshTokens.revokeAllByUserId(user.getId());
        recordEvent("PASSWORD_RESET_COMPLETED", user);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken, User user) {}
}
