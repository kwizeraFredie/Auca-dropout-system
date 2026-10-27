package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.Enrollment;

import java.util.UUID;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countByStudentIdAndSemesterId(UUID studentId, Long semesterId);
    boolean existsByStudentIdAndCourseIdAndSemesterId(UUID studentId, Long courseId, Long semesterId);
    List<Enrollment> findByStudentId(UUID studentId);
    List<Enrollment> findByStudentIdOrderBySemesterIdAscCourseIdAsc(UUID studentId);
    List<Enrollment> findByRiskFlaggedTrue();
    List<Enrollment> findByCourseId(Long courseId);
    java.util.Optional<Enrollment> findByStudentIdAndCourseIdAndSemesterId(UUID studentId, Long courseId, Long semesterId);
    long countByRiskFlaggedTrue();
}
