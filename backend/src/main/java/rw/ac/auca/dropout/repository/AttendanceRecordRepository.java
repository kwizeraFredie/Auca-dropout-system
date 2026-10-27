package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.AttendanceRecord;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    long countByStudentIdAndSessionId(UUID studentId, Long sessionId);
    long countByStudentIdAndPresentTrue(UUID studentId);
    long countByStudentId(UUID studentId);
    boolean existsBySessionIdAndStudentId(Long sessionId, UUID studentId);
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, UUID studentId);

    @Query("""
        select count(record) from AttendanceRecord record
        join AttendanceSession session on session.id = record.sessionId
        where record.studentId = :studentId and session.courseId = :courseId
          and session.semesterId = :semesterId and record.present = true
        """)
    long countPresentForCourseAndSemester(@Param("studentId") UUID studentId,
                                          @Param("courseId") Long courseId,
                                          @Param("semesterId") Long semesterId);

    @Query("""
        select count(record) from AttendanceRecord record
        join AttendanceSession session on session.id = record.sessionId
        where record.studentId = :studentId and session.courseId = :courseId
          and session.semesterId = :semesterId
        """)
    long countRecordedForCourseAndSemester(@Param("studentId") UUID studentId,
                                           @Param("courseId") Long courseId,
                                           @Param("semesterId") Long semesterId);
}
