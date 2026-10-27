package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.SecurityEvent;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
