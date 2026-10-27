package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.DepartmentRecord;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentRecord, Long> {
    Optional<DepartmentRecord> findByCode(String code);
}
