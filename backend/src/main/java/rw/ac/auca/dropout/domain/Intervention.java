package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interventions")
public class Intervention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private java.util.UUID studentId;
    @Column(name = "advisor_id", nullable = false)
    private java.util.UUID advisorId;
    @Column(nullable = false)
    private String notes;
    private String outcome;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        status = "OPEN";
    }

    public Long getId() { return id; }
    public java.util.UUID getStudentId() { return studentId; }
    public java.util.UUID getAdvisorId() { return advisorId; }
    public String getNotes() { return notes; }
    public String getOutcome() { return outcome; }
    public String getStatus() { return status; }
    public void setStudentId(java.util.UUID value) { studentId = value; }
    public void setAdvisorId(java.util.UUID value) { advisorId = value; }
    public void setNotes(String value) { notes = value; }
    public void setOutcome(String value) { outcome = value; }
    public void setStatus(String value) { status = value; }
    public void setResolvedAt(Instant value) { resolvedAt = value; }
}
