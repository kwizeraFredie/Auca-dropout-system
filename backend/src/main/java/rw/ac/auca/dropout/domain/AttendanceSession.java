package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "semester_id", "week_start"}))
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "semester_id", nullable = false)
    private Long semesterId;
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getSemesterId() { return semesterId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setCourseId(Long value) { courseId = value; }
    public void setSemesterId(Long value) { semesterId = value; }
    public void setSessionDate(LocalDate value) { sessionDate = value; }
    public void setWeekStart(LocalDate value) { weekStart = value; }
    public void setCreatedBy(UUID value) { createdBy = value; }
}
