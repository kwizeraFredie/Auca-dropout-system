package rw.ac.auca.dropout.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.security.AuthCookieService;
import rw.ac.auca.dropout.service.AuthService;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService cookies;
    private final UserRepository users;

    public AuthController(AuthService authService, AuthCookieService cookies, UserRepository users) {
        this.authService = authService;
        this.cookies = cookies;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegistrationRequest request, HttpServletResponse response) {
        User user = authService.registerStudent(
                request.email(), request.password(), request.firstName(), request.lastName(),
                request.gender(), request.studentNumber(), request.department());
        var tokens = authService.issueTokens(user);
        cookies.write(response, tokens);
        return AuthResponse.from(user);
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        User user = authService.authenticate(request.email(), request.password());
        var tokens = authService.issueTokens(user);
        cookies.write(response, tokens);
        return AuthResponse.from(user);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        var tokens = authService.refresh(requiredRefreshToken(request));
        cookies.write(response, tokens);
        return AuthResponse.from(tokens.user());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookies.refreshToken(request);
        if (refreshToken != null) authService.revoke(refreshToken);
        cookies.clear(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AuthResponse me(@AuthenticationPrincipal java.util.UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        return AuthResponse.from(user);
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return Map.of("message", "If the account exists, a password reset email has been sent");
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return Map.of("message", "Password reset completed");
    }

    public record RegistrationRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Size(max = 30) String gender,
            @Size(max = 40) String studentNumber,
            @NotNull rw.ac.auca.dropout.domain.Department department) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record PasswordResetRequest(@NotBlank @Email String email) {}

    public record PasswordResetConfirmRequest(@NotBlank String token,
                                              @NotBlank @Size(min = 12, max = 72) String newPassword) {}

    public record AuthResponse(Map<String, Object> user) {
        static AuthResponse from(User user) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("id", user.getId());
            details.put("email", user.getEmail());
            details.put("firstName", user.getFirstName());
            details.put("lastName", user.getLastName());
            details.put("role", user.getRole().name());
            details.put("department", user.getDepartment());
            details.put("accountLocked", user.isAccountLocked());
            return new AuthResponse(details);
        }
    }

    private String requiredRefreshToken(HttpServletRequest request) {
        String refreshToken = cookies.refreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        return refreshToken;
    }
}
