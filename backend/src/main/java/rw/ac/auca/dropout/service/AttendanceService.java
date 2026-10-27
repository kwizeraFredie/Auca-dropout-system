package rw.ac.auca.dropout.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.*;
import rw.ac.auca.dropout.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Service
public class AttendanceService {
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final CourseRepository courses;
    private final SemesterRepository semesters;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;

    public AttendanceService(AttendanceSessionRepository sessions, AttendanceRecordRepository records,
                             CourseRepository courses, SemesterRepository semesters,
                             EnrollmentRepository enrollments, UserRepository users) {
        this.sessions = sessions;
        this.records = records;
        this.courses = courses;
        this.semesters = semesters;
        this.enrollments = enrollments;
        this.users = users;
    }

    @Transactional
    public AttendanceSession createSession(UUID professorId, Long courseId, Long semesterId, LocalDate date) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course does not exist"));
        if (!professorId.equals(course.getProfessorId())) {
            throw new SecurityException("You are not assigned to this course");
        }
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester does not exist"));
        if (date.isBefore(semester.getStartsOn()) || date.isAfter(semester.getEndsOn())) {
            throw new IllegalArgumentException("Class date must fall within the semester");
        }
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (sessions.findByCourseIdAndSemesterIdAndWeekStart(courseId, semesterId, weekStart).isPresent()) {
            throw new IllegalStateException("This course already has an attendance session for that week");
        }
        AttendanceSession session = new AttendanceSession();
        session.setCourseId(courseId);
        session.setSemesterId(semesterId);
        session.setSessionDate(date);
        session.setWeekStart(weekStart);
        session.setCreatedBy(professorId);
        return sessions.save(session);
    }

    @Transactional
    public AttendanceRecord record(UUID professorId, Long sessionId, UUID studentId, boolean present) {
        AttendanceSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance session does not exist"));
        Course course = courses.findById(session.getCourseId()).orElseThrow();
        if (!professorId.equals(course.getProfessorId())) {
            throw new SecurityException("You are not assigned to this course");
        }
        enrollments.findByStudentIdAndCourseIdAndSemesterId(studentId, session.getCourseId(), session.getSemesterId())
                .orElseThrow(() -> new IllegalArgumentException("Student is not enrolled in this course"));
        AttendanceRecord record = records.findBySessionIdAndStudentId(sessionId, studentId)
                .orElseGet(AttendanceRecord::new);
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setPresent(present);
        AttendanceRecord saved = records.save(record);
        recalculateEnrollment(studentId, session.getCourseId(), session.getSemesterId());
        return saved;
    }

    private void recalculateEnrollment(UUID studentId, Long courseId, Long semesterId) {
        var enrollment = enrollments.findByStudentIdAndCourseIdAndSemesterId(studentId, courseId, semesterId).orElseThrow();
        long recorded = records.countRecordedForCourseAndSemester(studentId, courseId, semesterId);
        long present = records.countPresentForCourseAndSemester(studentId, courseId, semesterId);
        BigDecimal attendance = recorded == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(present * 100.0 / recorded).setScale(2, RoundingMode.HALF_UP);
        enrollment.setAttendancePercent(attendance);
        BigDecimal mark = calculateMark(enrollment);
        boolean atRisk = RiskPolicy.isAtRisk(attendance, mark);
        enrollment.setRiskFlagged(atRisk);
        if (atRisk) users.findById(studentId).ifPresent(student -> {
            student.setAccountLocked(true);
            users.save(student);
        });
        enrollments.save(enrollment);
    }

    private BigDecimal calculateMark(Enrollment enrollment) {
        if (enrollment.getAssignmentMark() == null || enrollment.getCatMark() == null
                || enrollment.getFinalExamMark() == null) return null;
        return enrollment.getAssignmentMark().multiply(BigDecimal.valueOf(.20))
                .add(enrollment.getCatMark().multiply(BigDecimal.valueOf(.30)))
                .add(enrollment.getFinalExamMark().multiply(BigDecimal.valueOf(.50)));
    }
}
