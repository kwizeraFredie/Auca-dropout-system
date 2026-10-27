package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.Semester;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findFirstByActiveTrue();
}
