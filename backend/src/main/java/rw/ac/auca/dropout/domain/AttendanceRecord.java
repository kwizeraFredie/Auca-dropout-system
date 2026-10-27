package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    @Column(name = "student_id", nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private boolean present;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    void onCreate() { recordedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }
    public boolean isPresent() { return present; }
    public void setSessionId(Long value) { sessionId = value; }
    public void setStudentId(UUID value) { studentId = value; }
    public void setPresent(boolean value) { present = value; }
}
