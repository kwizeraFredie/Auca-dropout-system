package rw.ac.auca.dropout.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.Enrollment;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.EnrollmentRepository;
import rw.ac.auca.dropout.service.RiskService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transcripts")
public class TranscriptController {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final RiskService riskService;

    public TranscriptController(EnrollmentRepository enrollments, CourseRepository courses, RiskService riskService) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.riskService = riskService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<TranscriptRow> transcript(@AuthenticationPrincipal UUID studentId) {
        return enrollments.findByStudentIdOrderBySemesterIdAscCourseIdAsc(studentId).stream()
                .map(this::toRow)
                .toList();
    }

    @GetMapping(value = "/download", produces = "text/csv")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<String> download(@AuthenticationPrincipal UUID studentId) {
        StringBuilder csv = new StringBuilder("Semester,Course code,Course title,Attendance %,Course mark,Status\n");
        transcript(studentId).forEach(row -> csv.append(csv(row.semesterId())).append(',')
                .append(csv(row.courseCode())).append(',')
                .append(csv(row.courseTitle())).append(',')
                .append(value(row.attendancePercent())).append(',')
                .append(value(row.courseMark())).append(',')
                .append(csv(row.status())).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auca-transcript.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private TranscriptRow toRow(Enrollment enrollment) {
        var course = courses.findById(enrollment.getCourseId()).orElse(null);
        BigDecimal mark = riskService.calculateMark(enrollment);
        String status = enrollment.isRiskFlagged() ? "AT RISK" : mark == null ? "IN PROGRESS" : "PASS";
        return new TranscriptRow(enrollment.getSemesterId(), course == null ? "UNKNOWN" : course.getCode(),
                course == null ? "Unknown course" : course.getTitle(), enrollment.getAttendancePercent(), mark, status);
    }

    private static String csv(Object value) {
        if (value == null) return "";
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    public record TranscriptRow(Long semesterId, String courseCode, String courseTitle,
                                BigDecimal attendancePercent, BigDecimal courseMark, String status) {}
}
