package rw.ac.auca.dropout.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.Course;
import rw.ac.auca.dropout.service.EnrollmentService;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.EnrollmentRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseRepository courses;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollments;

    public CourseController(CourseRepository courses, EnrollmentService enrollmentService,
                            EnrollmentRepository enrollments) {
        this.courses = courses;
        this.enrollmentService = enrollmentService;
        this.enrollments = enrollments;
    }

    @GetMapping
    public List<Course> listCourses() {
        return courses.findAll();
    }

    @PostMapping("/{courseId}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResponse enroll(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UUID studentId) {
        var enrollment = enrollmentService.enroll(studentId, courseId);
        return new EnrollmentResponse(enrollment.getId(), enrollment.getCourseId(), enrollment.getSemesterId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentSummary> myEnrollments(@AuthenticationPrincipal UUID studentId) {
        return enrollments.findByStudentIdOrderBySemesterIdAscCourseIdAsc(studentId).stream()
                .map(enrollment -> new EnrollmentSummary(
                        enrollment.getId(), enrollment.getCourseId(), enrollment.getSemesterId(),
                        enrollment.getAttendancePercent(), enrollment.isRiskFlagged()))
                .toList();
    }

    public record EnrollmentResponse(Long id, Long courseId, Long semesterId) {}
    public record EnrollmentSummary(Long id, Long courseId, Long semesterId,
                                    java.math.BigDecimal attendancePercent, boolean atRisk) {}
}
