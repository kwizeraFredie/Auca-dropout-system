package rw.ac.auca.dropout.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.EnrollmentRepository;
import rw.ac.auca.dropout.repository.InterventionRepository;
import rw.ac.auca.dropout.repository.DepartmentRepository;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.domain.Enrollment;
import rw.ac.auca.dropout.domain.Intervention;
import rw.ac.auca.dropout.domain.RiskPolicy;
import rw.ac.auca.dropout.domain.Role;
import rw.ac.auca.dropout.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
import java.util.Set;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final InterventionRepository interventions;
    private final DepartmentRepository departments;
    private final UserRepository users;

    public ReportController(EnrollmentRepository enrollments, CourseRepository courses,
                            InterventionRepository interventions, DepartmentRepository departments,
                            UserRepository users) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.interventions = interventions;
        this.departments = departments;
        this.users = users;
    }

    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ReportOverview overview(@AuthenticationPrincipal UUID requesterId) {
        User requester = user(requesterId);
        List<Enrollment> atRisk = scopedRisk(requester);
        List<rw.ac.auca.dropout.domain.Course> visibleCourses = scopedCourses(requester);
        List<Intervention> visibleInterventions = scopedInterventions(requester);
        Map<Long, String> departmentNames = departments.findAll().stream()
                .collect(Collectors.toMap(record -> record.getId(), record -> record.getName()));
        Map<Long, String> courseDepartments = visibleCourses.stream()
                .collect(Collectors.toMap(course -> course.getId(),
                        course -> departmentNames.getOrDefault(course.getDepartmentId(), "Unknown")));
        Map<String, Long> byDepartment = atRisk.stream().collect(Collectors.groupingBy(
                enrollment -> courseDepartments.getOrDefault(enrollment.getCourseId(), "Unknown"),
                Collectors.counting()));
        long attendanceRisk = atRisk.stream().filter(this::attendanceRisk).count();
        long marksRisk = atRisk.stream().filter(this::marksRisk).count();
        Map<Long, Long> bySemester = atRisk.stream().collect(Collectors.groupingBy(
                Enrollment::getSemesterId, Collectors.counting()));
        Map<String, Long> interventionStatuses = visibleInterventions.stream()
                .collect(Collectors.groupingBy(Intervention::getStatus, Collectors.counting()));
        long interventionCount = visibleInterventions.size();
        long resolvedInterventions = visibleInterventions.stream()
                .filter(intervention -> "RESOLVED".equals(intervention.getStatus())).count();
        List<RiskRow> recentRiskFlags = atRisk.stream()
                .sorted(Comparator.comparing(Enrollment::getId).reversed())
                .limit(5)
                .map(enrollment -> new RiskRow(enrollment.getId(), enrollment.getStudentId(),
                        enrollment.getCourseId(), attendanceRisk(enrollment) ? "Attendance below 70%" : "Course mark below 50%"))
                .toList();
        return new ReportOverview(
                visibleUsers(requester),
                visibleCourses.size(),
                atRisk.size(),
                interventionCount == 0 ? 0 : Math.round((resolvedInterventions * 100.0) / interventionCount),
                recentRiskFlags,
                byDepartment.entrySet().stream().map(entry -> new ChartPoint(entry.getKey(), entry.getValue())).toList(),
                List.of(new ChartPoint("Attendance below 70%", attendanceRisk),
                        new ChartPoint("Course mark below 50%", marksRisk)),
                bySemester.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(entry -> new ChartPoint("Semester " + entry.getKey(), entry.getValue())).toList(),
                interventionStatuses.entrySet().stream()
                        .map(entry -> new ChartPoint(entry.getKey(), entry.getValue())).toList());
    }

    @GetMapping(value = "/at-risk.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'ADVISOR')")
    public ResponseEntity<String> atRiskCsv(@AuthenticationPrincipal UUID requesterId) {
        User requester = user(requesterId);
        StringBuilder csv = new StringBuilder("Enrollment ID,Student ID,Course ID,Course code,Attendance %,Course mark,Risk reason,Risk status\n");
        scopedRisk(requester).forEach(enrollment -> {
            var course = courses.findById(enrollment.getCourseId()).orElse(null);
            String reason = attendanceRisk(enrollment) ? "Attendance below 70%" : "Course mark below 50%";
            csv.append(enrollment.getId()).append(',')
                    .append(enrollment.getStudentId()).append(',')
                    .append(enrollment.getCourseId()).append(',')
                    .append(csvValue(course == null ? "UNKNOWN" : course.getCode())).append(',')
                    .append(enrollment.getAttendancePercent() == null ? "" : enrollment.getAttendancePercent()).append(',')
                    .append(courseMark(enrollment) == null ? "" : courseMark(enrollment)).append(',')
                    .append(csvValue(reason)).append(',')
                    .append("AT RISK\n");
        });
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auca-at-risk-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean attendanceRisk(Enrollment enrollment) {
        return enrollment.getAttendancePercent() != null
                && enrollment.getAttendancePercent().compareTo(RiskPolicy.MINIMUM_ATTENDANCE_PERCENT) < 0;
    }

    private User user(UUID id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }

    private List<Enrollment> scopedRisk(User requester) {
        List<Enrollment> risk = enrollments.findByRiskFlaggedTrue();
        if (requester.getRole() == Role.ADMIN) return risk;
        if (requester.getRole() == Role.ADVISOR) {
            Set<UUID> students = interventions.findByAdvisorId(requester.getId()).stream()
                    .map(Intervention::getStudentId).collect(Collectors.toSet());
            return risk.stream().filter(row -> students.contains(row.getStudentId())).toList();
        }
        if (requester.getRole() == Role.STUDENT) {
            return risk.stream().filter(row -> row.getStudentId().equals(requester.getId())).toList();
        }
        Set<Long> courseIds = scopedCourses(requester).stream().map(rw.ac.auca.dropout.domain.Course::getId)
                .collect(Collectors.toSet());
        return risk.stream().filter(row -> courseIds.contains(row.getCourseId())).toList();
    }

    private List<rw.ac.auca.dropout.domain.Course> scopedCourses(User requester) {
        List<rw.ac.auca.dropout.domain.Course> visible = courses.findAll();
        if (requester.getRole() == Role.ADMIN) return visible;
        if (requester.getRole() == Role.PROFESSOR) {
            return visible.stream().filter(course -> requester.getId().equals(course.getProfessorId())).toList();
        }
        if (requester.getRole() == Role.STUDENT) {
            Set<Long> enrolled = enrollments.findByStudentId(requester.getId()).stream()
                    .map(Enrollment::getCourseId).collect(Collectors.toSet());
            return visible.stream().filter(course -> enrolled.contains(course.getId())).toList();
        }
        if (requester.getRole() == Role.HOD) {
            String code = requester.getDepartment() == null ? "" : requester.getDepartment().name();
            Set<Long> departmentIds = departments.findAll().stream()
                    .filter(department -> code.equals(department.getCode()))
                    .map(record -> record.getId()).collect(Collectors.toSet());
            return visible.stream().filter(course -> departmentIds.contains(course.getDepartmentId())).toList();
        }
        return visible;
    }

    private List<Intervention> scopedInterventions(User requester) {
        if (requester.getRole() == Role.ADMIN) return interventions.findAll();
        if (requester.getRole() == Role.ADVISOR) return interventions.findByAdvisorId(requester.getId());
        if (requester.getRole() == Role.STUDENT) return interventions.findByStudentId(requester.getId());
        if (requester.getRole() == Role.HOD) {
            Set<UUID> students = users.findByRoleAndDepartment(Role.STUDENT, requester.getDepartment()).stream()
                    .map(User::getId).collect(Collectors.toSet());
            return interventions.findAll().stream().filter(row -> students.contains(row.getStudentId())).toList();
        }
        return List.of();
    }

    private long visibleUsers(User requester) {
        if (requester.getRole() == Role.ADMIN) return users.count();
        if (requester.getRole() == Role.HOD) return users.findByRoleAndDepartment(Role.STUDENT, requester.getDepartment()).size();
        if (requester.getRole() == Role.ADVISOR) return interventions.findByAdvisorId(requester.getId()).stream()
                .map(Intervention::getStudentId).distinct().count();
        if (requester.getRole() == Role.STUDENT) return 1;
        return scopedCourses(requester).stream().map(rw.ac.auca.dropout.domain.Course::getId).distinct().count();
    }

    private boolean marksRisk(Enrollment enrollment) {
        BigDecimal mark = courseMark(enrollment);
        return mark != null && mark.compareTo(RiskPolicy.PASSING_MARK_PERCENT) < 0;
    }

    private BigDecimal courseMark(Enrollment enrollment) {
        if (enrollment.getAssignmentMark() == null || enrollment.getCatMark() == null
                || enrollment.getFinalExamMark() == null) return null;
        return enrollment.getAssignmentMark().multiply(BigDecimal.valueOf(.20))
                .add(enrollment.getCatMark().multiply(BigDecimal.valueOf(.30)))
                .add(enrollment.getFinalExamMark().multiply(BigDecimal.valueOf(.50)));
    }

    public record ChartPoint(String name, long value) {}
    public record RiskRow(Long enrollmentId, java.util.UUID studentId, Long courseId, String reason) {}
    public record ReportOverview(long totalStudents, long activeCourses, long totalAtRisk,
                                 long resolvedInterventionPercent, List<RiskRow> recentRiskFlags,
                                 List<ChartPoint> byDepartment,
                                 List<ChartPoint> riskCauses, List<ChartPoint> trend,
                                 List<ChartPoint> interventionStatuses) {}
}
