package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id", "semester_id"}))
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private java.util.UUID studentId;
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(name = "semester_id", nullable = false)
    private Long semesterId;
    @Column(name = "attendance_percent")
    private BigDecimal attendancePercent;
    @Column(name = "assignment_mark")
    private BigDecimal assignmentMark;
    @Column(name = "cat_mark")
    private BigDecimal catMark;
    @Column(name = "final_exam_mark")
    private BigDecimal finalExamMark;
    @Column(name = "risk_flagged", nullable = false)
    private boolean riskFlagged;

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getSemesterId() { return semesterId; }
    public boolean isRiskFlagged() { return riskFlagged; }
    public java.util.UUID getStudentId() { return studentId; }
    public BigDecimal getAttendancePercent() { return attendancePercent; }
    public BigDecimal getAssignmentMark() { return assignmentMark; }
    public BigDecimal getCatMark() { return catMark; }
    public BigDecimal getFinalExamMark() { return finalExamMark; }
    public BigDecimal getCourseMark() {
        if (assignmentMark == null || catMark == null || finalExamMark == null) return null;
        return assignmentMark.multiply(BigDecimal.valueOf(.20))
                .add(catMark.multiply(BigDecimal.valueOf(.30)))
                .add(finalExamMark.multiply(BigDecimal.valueOf(.50)))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public void setAttendancePercent(BigDecimal value) { attendancePercent = value; }
    public void setAssignmentMark(BigDecimal value) { assignmentMark = value; }
    public void setCatMark(BigDecimal value) { catMark = value; }
    public void setFinalExamMark(BigDecimal value) { finalExamMark = value; }
    public void setRiskFlagged(boolean value) { riskFlagged = value; }
    public void setStudentId(java.util.UUID value) { studentId = value; }
    public void setCourseId(Long value) { courseId = value; }
    public void setSemesterId(Long value) { semesterId = value; }
}
