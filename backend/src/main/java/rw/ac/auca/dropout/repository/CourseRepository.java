package rw.ac.auca.dropout.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.dropout.domain.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
