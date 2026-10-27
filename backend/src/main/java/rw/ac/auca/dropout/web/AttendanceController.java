package rw.ac.auca.dropout.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.AttendanceRecord;
import rw.ac.auca.dropout.domain.AttendanceSession;
import rw.ac.auca.dropout.service.AttendanceService;
import rw.ac.auca.dropout.repository.AttendanceSessionRepository;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendance;
    private final AttendanceSessionRepository sessions;

    public AttendanceController(AttendanceService attendance, AttendanceSessionRepository sessions) {
        this.attendance = attendance;
        this.sessions = sessions;
    }

    @GetMapping("/sessions/mine")
    @PreAuthorize("hasRole('PROFESSOR')")
    public java.util.List<AttendanceSession> mySessions(@AuthenticationPrincipal UUID professorId) {
        return sessions.findByCreatedByOrderBySessionDateDesc(professorId);
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AttendanceSession createSession(@AuthenticationPrincipal UUID professorId,
                                           @Valid @RequestBody SessionRequest request) {
        return attendance.createSession(professorId, request.courseId(), request.semesterId(), request.sessionDate());
    }

    @PutMapping("/sessions/{sessionId}/students/{studentId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AttendanceRecord record(@AuthenticationPrincipal UUID professorId,
                                   @PathVariable Long sessionId,
                                   @PathVariable UUID studentId,
                                   @Valid @RequestBody AttendanceRequest request) {
        return attendance.record(professorId, sessionId, studentId, request.present());
    }

    public record SessionRequest(@NotNull Long courseId, @NotNull Long semesterId,
                                 @NotNull LocalDate sessionDate) {}
    public record AttendanceRequest(@NotNull Boolean present) {}
}
