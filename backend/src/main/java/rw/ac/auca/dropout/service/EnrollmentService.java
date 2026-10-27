package rw.ac.auca.dropout.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.Enrollment;
import rw.ac.auca.dropout.domain.RiskPolicy;
import rw.ac.auca.dropout.repository.CourseRepository;
import rw.ac.auca.dropout.repository.EnrollmentRepository;
import rw.ac.auca.dropout.repository.SemesterRepository;

import java.util.UUID;

@Service
public class EnrollmentService {
    private final CourseRepository courses;
    private final SemesterRepository semesters;
    private final EnrollmentRepository enrollments;

    public EnrollmentService(CourseRepository courses, SemesterRepository semesters, EnrollmentRepository enrollments) {
        this.courses = courses;
        this.semesters = semesters;
        this.enrollments = enrollments;
    }

    @Transactional
    public Enrollment enroll(UUID studentId, Long courseId) {
        var semester = semesters.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("There is no active semester"));
        if (!courses.existsById(courseId)) {
            throw new IllegalArgumentException("Course does not exist");
        }
        if (enrollments.existsByStudentIdAndCourseIdAndSemesterId(studentId, courseId, semester.getId())) {
            throw new IllegalStateException("You are already enrolled in this course");
        }
        if (enrollments.countByStudentIdAndSemesterId(studentId, semester.getId())
                >= RiskPolicy.MAX_COURSES_PER_SEMESTER) {
            throw new IllegalStateException("Students may enroll in at most six courses per semester");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setSemesterId(semester.getId());
        return enrollments.save(enrollment);
    }
}
