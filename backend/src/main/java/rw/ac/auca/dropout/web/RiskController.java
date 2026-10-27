package rw.ac.auca.dropout.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.Enrollment;
import rw.ac.auca.dropout.service.RiskService;
import rw.ac.auca.dropout.repository.EnrollmentRepository;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.DepartmentRepository;
import rw.ac.auca.dropout.repository.InterventionRepository;
import rw.ac.auca.dropout.domain.Role;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskService riskService;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final InterventionRepository interventions;

    public RiskController(RiskService riskService, EnrollmentRepository enrollments,
                          UserRepository users, CourseRepository courses, DepartmentRepository departments,
                          InterventionRepository interventions) {
        this.riskService = riskService;
        this.enrollments = enrollments;
        this.users = users;
        this.courses = courses;
        this.departments = departments;
        this.interventions = interventions;
    }

    @GetMapping("/at-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'ADVISOR')")
    public java.util.List<Enrollment> atRisk(@AuthenticationPrincipal UUID requesterId) {
        var requester = users.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        var risk = enrollments.findByRiskFlaggedTrue();
        if (requester.getRole() == Role.ADMIN) return risk;
        if (requester.getRole() == Role.ADVISOR) {
            var studentIds = interventions.findByAdvisorId(requesterId).stream()
                    .map(intervention -> intervention.getStudentId()).collect(java.util.stream.Collectors.toSet());
            return risk.stream().filter(row -> studentIds.contains(row.getStudentId())).toList();
        }
        if (requester.getRole() == Role.HOD) {
            var departmentCodes = departments.findAll().stream()
                    .filter(department -> department.getCode().equals(requester.getDepartment().name()))
                    .map(department -> department.getId()).toList();
            return risk.stream().filter(row -> courses.findById(row.getCourseId())
                    .map(course -> departmentCodes.contains(course.getDepartmentId())).orElse(false)).toList();
        }
        return risk;
    }

    @GetMapping("/professor/enrollments")
    @PreAuthorize("hasRole('PROFESSOR')")
    public java.util.List<Enrollment> professorEnrollments(@AuthenticationPrincipal UUID professorId) {
        return riskService.professorEnrollments(professorId);
    }

    @PutMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public RiskResponse updateRecord(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal UUID professorId,
            @Valid @RequestBody AcademicRecordRequest request) {
        Enrollment enrollment = riskService.updateAcademicRecord(professorId, enrollmentId,
                new RiskService.AcademicRecord(request.assignmentMark(), request.catMark(), request.finalExamMark()));
        return new RiskResponse(enrollment.getId(), enrollment.isRiskFlagged(),
                riskService.calculateMark(enrollment));
    }

    public record AcademicRecordRequest(
            @DecimalMin("0") @DecimalMax("100") BigDecimal assignmentMark,
            @DecimalMin("0") @DecimalMax("100") BigDecimal catMark,
            @DecimalMin("0") @DecimalMax("100") BigDecimal finalExamMark) {}

    public record RiskResponse(Long enrollmentId, boolean atRisk, BigDecimal courseMark) {}
}
