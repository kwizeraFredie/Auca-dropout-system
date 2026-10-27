package rw.ac.auca.dropout.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rw.ac.auca.dropout.domain.Department;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.service.AuthService;
import rw.ac.auca.dropout.security.AuthCookieService;


import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(new StubAuthService(),
                new AuthCookieService(false), null))
            .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void rejectsMalformedLoginRequest() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void resetRequestDoesNotRevealWhetherEmailExists() throws Exception {
        mvc.perform(post("/api/auth/password-reset/request")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", equalTo(
                        "If the account exists, a password reset email has been sent")));
    }

    @Test
    void refreshRequiresARefreshToken() throws Exception {
        mvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static class StubAuthService extends AuthService {
        StubAuthService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public void requestPasswordReset(String email) {
        }

        @Override
        public AuthTokens refresh(String token) {
            return new AuthTokens("access", "refresh", user());
        }

        @Override
        public User registerStudent(String email, String password, String firstName, String lastName,
                                    String gender, String studentNumber, Department department) {
            return user();
        }

        @Override
        public AuthTokens issueTokens(User user) {
            return new AuthTokens("access", "refresh", user);
        }

        private User user() {
            User user = new User();
            user.setEmail("student@example.com");
            user.setFirstName("Test");
            user.setLastName("Student");
            user.setDepartment(Department.SOFTWARE_ENGINEERING);
            user.setRole(rw.ac.auca.dropout.domain.Role.STUDENT);
            return user;
        }
    }
}
