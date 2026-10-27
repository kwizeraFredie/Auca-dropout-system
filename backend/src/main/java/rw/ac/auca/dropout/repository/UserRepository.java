package rw.ac.auca.dropout.repository;

import rw.ac.auca.dropout.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import rw.ac.auca.dropout.domain.Role;
import rw.ac.auca.dropout.domain.Department;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndDepartment(Role role, Department department);
}
