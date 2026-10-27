package rw.ac.auca.dropout.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.*;
import rw.ac.auca.dropout.repository.*;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AdministrationService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final SemesterRepository semesters;
    private final DepartmentRepository departments;
    private final PasswordEncoder passwordEncoder;

    public AdministrationService(UserRepository users, CourseRepository courses, SemesterRepository semesters,
                                 DepartmentRepository departments, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.courses = courses;
        this.semesters = semesters;
        this.departments = departments;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createStaff(StaffRequest request) {
        if (request.role() == Role.STUDENT) throw new IllegalArgumentException("Students must self-register");
        if (request.role() != Role.ADMIN && request.department() == null) {
            throw new IllegalArgumentException("A department is required for this staff role");
        }
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("An account already exists for this email");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setGender(request.gender());
        user.setRole(request.role());
        user.setDepartment(request.department());
        user.setAccountLocked(false);
        return users.save(user);
    }

    @Transactional
    public Semester createSemester(SemesterRequest request) {
        if (!request.startsOn().plusMonths(3).equals(request.endsOn())) {
            throw new IllegalArgumentException("Each semester must run for exactly three months");
        }
        if (request.active()) semesters.findAll().forEach(existing -> existing.setActive(false));
        Semester semester = new Semester();
        semester.setName(request.name().trim());
        semester.setAcademicYear(request.academicYear().trim());
        semester.setStartsOn(request.startsOn());
        semester.setEndsOn(request.endsOn());
        semester.setActive(request.active());
        return semesters.save(semester);
    }

    @Transactional
    public Course createCourse(UUID hodId, CourseRequest request) {
        User hod = requireRole(hodId, Role.HOD);
        DepartmentRecord department = departments.findById(request.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department does not exist"));
        if (hod.getDepartment() == null || !hod.getDepartment().name().equals(department.getCode())) {
            throw new SecurityException("HODs may only manage their own department");
        }
        Course course = new Course();
        course.setCode(request.code().trim().toUpperCase());
        course.setTitle(request.title().trim());
        course.setCredits(request.credits());
        course.setDepartmentId(department.getId());
        return courses.save(course);
    }

    @Transactional
    public Course assignProfessor(UUID hodId, Long courseId, UUID professorId) {
        User hod = requireRole(hodId, Role.HOD);
        User professor = requireRole(professorId, Role.PROFESSOR);
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course does not exist"));
        DepartmentRecord department = departments.findById(course.getDepartmentId()).orElseThrow();
        if (hod.getDepartment() == null || !hod.getDepartment().name().equals(department.getCode())) {
            throw new SecurityException("HODs may only manage their own department");
        }
        if (professor.getDepartment() == null
                || !professor.getDepartment().name().equals(department.getCode())) {
            throw new IllegalArgumentException("Professor must belong to the course department");
        }
        course.setProfessorId(professorId);
        return courses.save(course);
    }

    private User requireRole(UUID id, Role role) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (user.getRole() != role) throw new SecurityException("User does not have the required role");
        return user;
    }

    public record StaffRequest(String email, String temporaryPassword, String firstName, String lastName,
                               String gender, Role role, Department department) {}
    public record SemesterRequest(String name, String academicYear, LocalDate startsOn,
                                  LocalDate endsOn, boolean active) {}
    public record CourseRequest(String code, String title, int credits, Long departmentId) {}
}
