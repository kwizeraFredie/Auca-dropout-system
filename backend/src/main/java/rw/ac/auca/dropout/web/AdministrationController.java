package rw.ac.auca.dropout.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.*;
import rw.ac.auca.dropout.repository.*;
import rw.ac.auca.dropout.service.AdministrationService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdministrationController {
    private final AdministrationService administration;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final SemesterRepository semesters;

    public AdministrationController(AdministrationService administration, UserRepository users,
                                    DepartmentRepository departments, SemesterRepository semesters) {
        this.administration = administration;
        this.users = users;
        this.departments = departments;
        this.semesters = semesters;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummary> users() {
        return users.findAll().stream().map(UserSummary::from).toList();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary createStaff(@Valid @RequestBody StaffRequest request) {
        return UserSummary.from(administration.createStaff(new AdministrationService.StaffRequest(
                request.email(), request.temporaryPassword(), request.firstName(), request.lastName(),
                request.gender(), request.role(), request.department())));
    }

    @GetMapping("/departments")
    public List<DepartmentRecord> departments() {
        return departments.findAll();
    }

    @GetMapping("/advisors")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public List<UserSummary> advisors(@AuthenticationPrincipal UUID requesterId) {
        User requester = users.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        List<User> advisors = requester.getRole() == Role.HOD && requester.getDepartment() != null
                ? users.findByRoleAndDepartment(Role.ADVISOR, requester.getDepartment())
                : users.findByRole(Role.ADVISOR);
        return advisors.stream().map(UserSummary::from).toList();
    }

    @GetMapping("/professors")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public List<UserSummary> professors(@AuthenticationPrincipal UUID requesterId) {
        User requester = users.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        List<User> professors = requester.getRole() == Role.HOD && requester.getDepartment() != null
                ? users.findByRoleAndDepartment(Role.PROFESSOR, requester.getDepartment())
                : users.findByRole(Role.PROFESSOR);
        return professors.stream().map(UserSummary::from).toList();
    }

    @PostMapping("/semesters")
    @PreAuthorize("hasRole('ADMIN')")
    public Semester createSemester(@Valid @RequestBody SemesterRequest request) {
        return administration.createSemester(new AdministrationService.SemesterRequest(
                request.name(), request.academicYear(), request.startsOn(), request.endsOn(), request.active()));
    }

    @GetMapping("/semesters")
    public List<Semester> semesters() {
        return semesters.findAll();
    }

    @PostMapping("/courses")
    @PreAuthorize("hasRole('HOD')")
    public Course createCourse(@AuthenticationPrincipal UUID hodId, @Valid @RequestBody CourseRequest request) {
        return administration.createCourse(hodId, new AdministrationService.CourseRequest(
                request.code(), request.title(), request.credits(), request.departmentId()));
    }

    @PutMapping("/courses/{courseId}/professor/{professorId}")
    @PreAuthorize("hasRole('HOD')")
    public Course assignProfessor(@AuthenticationPrincipal UUID hodId, @PathVariable Long courseId,
                                  @PathVariable UUID professorId) {
        return administration.assignProfessor(hodId, courseId, professorId);
    }

    public record StaffRequest(@NotBlank @Email String email, @NotBlank @Size(min = 12, max = 72) String temporaryPassword,
                               @NotBlank @Size(max = 80) String firstName, @NotBlank @Size(max = 80) String lastName,
                               @Size(max = 30) String gender, @NotNull Role role, Department department) {}
    public record SemesterRequest(@NotBlank String name, @NotBlank String academicYear,
                                  @NotNull LocalDate startsOn, @NotNull LocalDate endsOn, boolean active) {}
    public record CourseRequest(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 160) String title,
                                @Min(1) @Max(30) int credits, @NotNull Long departmentId) {}
    public record UserSummary(UUID id, String email, String firstName, String lastName, Role role,
                              Department department, boolean accountLocked) {
        static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getRole(), user.getDepartment(), user.isAccountLocked());
        }
    }
}
