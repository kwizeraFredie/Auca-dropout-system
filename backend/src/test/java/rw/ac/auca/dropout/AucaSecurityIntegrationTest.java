package rw.ac.auca.dropout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AucaSecurityIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("security.jwt.secret", () -> "integration-test-secret-that-is-long-enough");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:5173");
        registry.add("app.password-reset.from", () -> "test@example.edu");
        registry.add("app.password-reset.url", () -> "http://localhost:5173/reset-password");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "2525");
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
    }

    @Test
    void refreshRotatesAndRevokesTokensAndRolesAreEnforced() throws Exception {
                ResponseEntity<String> csrfResponse = rest.getForEntity(url("/api/auth/csrf"), String.class);
        String csrfToken = csrfResponse.getBody().replace("{\"token\":\"", "").replace("\"}", "");
                String csrfCookie = cookieValue(csrfResponse, "XSRF-TOKEN");
        String email = "integration-" + System.nanoTime() + "@example.com";
        ResponseEntity<String> register = rest.postForEntity(url("/api/auth/register"),
                Map.of("email", email, "password", "integration-password",
                        "firstName", "Integration", "lastName", "Student",
                        "department", "SOFTWARE_ENGINEERING"), String.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());
        String accessToken = cookieValue(register, "auca_access_token");
        String refreshToken = cookieValue(register, "auca_refresh_token");
        assertNotNull(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "auca_access_token=" + accessToken);
        ResponseEntity<String> forbidden = rest.exchange(url("/api/admin/users"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + csrfCookie + "; auca_refresh_token=" + refreshToken);
        refreshHeaders.add("X-XSRF-TOKEN", csrfToken);
        ResponseEntity<String> refreshed = rest.exchange(url("/api/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);
        assertEquals(HttpStatus.OK, refreshed.getStatusCode());
        String rotatedRefreshToken = cookieValue(refreshed, "auca_refresh_token");
        assertNotEquals(refreshToken, rotatedRefreshToken);

        ResponseEntity<String> reused = rest.exchange(url("/api/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, reused.getStatusCode());

        HttpHeaders rotatedHeaders = new HttpHeaders();
        rotatedHeaders.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + csrfCookie + "; auca_refresh_token=" + rotatedRefreshToken);
        rotatedHeaders.add("X-XSRF-TOKEN", csrfToken);
        ResponseEntity<Void> logout = rest.exchange(url("/api/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(rotatedHeaders), Void.class);
        assertEquals(HttpStatus.OK, logout.getStatusCode());

        ResponseEntity<String> afterLogout = rest.exchange(url("/api/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(rotatedHeaders), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, afterLogout.getStatusCode());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() {
        ResponseEntity<String> response = rest.getForEntity(url("/api/admin/users"), String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

        private String cookieValue(ResponseEntity<?> response, String name) {
                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                return cookies.stream()
                                .map(cookie -> cookie.split(";", 2)[0])
                                .filter(cookie -> cookie.startsWith(name + "="))
                                .map(cookie -> cookie.substring(name.length() + 1))
                                .findFirst()
                                .orElseThrow();
        }
}
