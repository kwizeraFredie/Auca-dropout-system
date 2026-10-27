package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.Intervention;

import java.util.List;
import java.util.UUID;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByAdvisorId(UUID advisorId);
    List<Intervention> findByStudentId(UUID studentId);
    List<Intervention> findByAdvisorIdOrderByCreatedAtDesc(UUID advisorId);
    List<Intervention> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    boolean existsByStudentIdAndStatus(UUID studentId, String status);
}
