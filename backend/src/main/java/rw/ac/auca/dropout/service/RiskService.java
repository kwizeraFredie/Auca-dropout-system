package rw.ac.auca.dropout.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.Enrollment;
import rw.ac.auca.dropout.domain.RiskPolicy;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.AttendanceRecordRepository;
import rw.ac.auca.dropout.repository.EnrollmentRepository;
import rw.ac.auca.dropout.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class RiskService {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final UserRepository users;
    private final AttendanceRecordRepository attendanceRecords;

    public RiskService(EnrollmentRepository enrollments, CourseRepository courses, UserRepository users,
                       AttendanceRecordRepository attendanceRecords) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.users = users;
        this.attendanceRecords = attendanceRecords;
    }

    @Transactional
    public Enrollment updateAcademicRecord(UUID professorId, Long enrollmentId, AcademicRecord record) {
        Enrollment enrollment = enrollments.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment does not exist"));
        var course = courses.findById(enrollment.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course does not exist"));
        if (course.getProfessorId() == null || !course.getProfessorId().equals(professorId)) {
            throw new SecurityException("You are not assigned to this course");
        }
        validatePercentage(record.assignmentMark(), "assignment");
        validatePercentage(record.catMark(), "CAT");
        validatePercentage(record.finalExamMark(), "final exam");
        long recorded = attendanceRecords.countRecordedForCourseAndSemester(
            enrollment.getStudentId(), enrollment.getCourseId(), enrollment.getSemesterId());
        long present = attendanceRecords.countPresentForCourseAndSemester(
            enrollment.getStudentId(), enrollment.getCourseId(), enrollment.getSemesterId());
        BigDecimal attendance = recorded == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(present * 100.0 / recorded).setScale(2, RoundingMode.HALF_UP);
        enrollment.setAttendancePercent(attendance);
        enrollment.setAssignmentMark(record.assignmentMark());
        enrollment.setCatMark(record.catMark());
        enrollment.setFinalExamMark(record.finalExamMark());
        BigDecimal mark = calculateMark(enrollment);
        boolean atRisk = RiskPolicy.isAtRisk(attendance, mark);
        enrollment.setRiskFlagged(atRisk);
        if (atRisk) {
            users.findById(enrollment.getStudentId()).ifPresent(student -> {
                student.setAccountLocked(true);
                users.save(student);
            });
        }
        return enrollments.save(enrollment);
    }

    public BigDecimal calculateMark(Enrollment enrollment) {
        if (enrollment.getAssignmentMark() == null || enrollment.getCatMark() == null
                || enrollment.getFinalExamMark() == null) {
            return null;
        }

        return enrollment.getAssignmentMark().multiply(BigDecimal.valueOf(.20))
                .add(enrollment.getCatMark().multiply(BigDecimal.valueOf(.30)))
                .add(enrollment.getFinalExamMark().multiply(BigDecimal.valueOf(.50)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public java.util.List<Enrollment> professorEnrollments(UUID professorId) {
        return courses.findAll().stream()
                .filter(course -> professorId.equals(course.getProfessorId()))
                .flatMap(course -> enrollments.findByCourseId(course.getId()).stream())
                .toList();
    }

    private void validatePercentage(BigDecimal value, String field) {
        if (value != null && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException(field + " must be between 0 and 100");
        }
    }

    public record AcademicRecord(BigDecimal assignmentMark, BigDecimal catMark,
                                 BigDecimal finalExamMark) {}
}
