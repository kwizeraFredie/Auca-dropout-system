package rw.ac.auca.dropout.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.ac.auca.dropout.domain.Department;
import rw.ac.auca.dropout.domain.Role;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.repository.InterventionRepository;
import rw.ac.auca.dropout.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterventionServiceTest {
    @Mock
    private InterventionRepository interventions;
    @Mock
    private UserRepository users;

    @Test
    void cannotUnlockWithoutResolvedIntervention() {
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User admin = user(Role.ADMIN, null);
        User student = user(Role.STUDENT, Department.SOFTWARE_ENGINEERING);
        when(users.findById(adminId)).thenReturn(Optional.of(admin));
        when(users.findById(studentId)).thenReturn(Optional.of(student));
        when(interventions.existsByStudentIdAndStatus(studentId, "RESOLVED")).thenReturn(false);

        InterventionService service = new InterventionService(interventions, users);

        assertThrows(IllegalStateException.class, () -> service.unlock(adminId, studentId));
        verify(users, never()).save(student);
    }

    @Test
    void advisorCannotAssignAcrossDepartments() {
        UUID hodId = UUID.randomUUID();
        UUID advisorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User hod = user(Role.HOD, Department.SOFTWARE_ENGINEERING);
        User advisor = user(Role.ADVISOR, Department.INFORMATION_MANAGEMENT);
        User student = user(Role.STUDENT, Department.SOFTWARE_ENGINEERING);
        when(users.findById(hodId)).thenReturn(Optional.of(hod));
        when(users.findById(advisorId)).thenReturn(Optional.of(advisor));
        when(users.findById(studentId)).thenReturn(Optional.of(student));

        InterventionService service = new InterventionService(interventions, users);

        assertThrows(SecurityException.class,
                () -> service.assign(hodId, advisorId, studentId, "Academic support"));
        verify(interventions, never()).save(any());
    }

    private User user(Role role, Department department) {
        User user = new User();
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }
}
