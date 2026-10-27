package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.AttendanceSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByCourseIdAndSemesterIdAndWeekStart(Long courseId, Long semesterId, LocalDate weekStart);
    List<AttendanceSession> findByCreatedByOrderBySessionDateDesc(UUID professorId);
    List<AttendanceSession> findByCourseIdAndSemesterId(Long courseId, Long semesterId);
}
