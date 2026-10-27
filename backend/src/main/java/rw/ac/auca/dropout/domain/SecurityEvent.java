package rw.ac.auca.dropout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email")
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public void setEventType(String value) { eventType = value; }
    public void setUserId(UUID value) { userId = value; }
    public void setEmail(String value) { email = value; }
    public void setCreatedAt(Instant value) { createdAt = value; }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
