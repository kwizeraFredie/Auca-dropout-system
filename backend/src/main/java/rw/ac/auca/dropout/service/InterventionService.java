package rw.ac.auca.dropout.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.dropout.domain.Intervention;
import rw.ac.auca.dropout.domain.User;
import rw.ac.auca.dropout.repository.InterventionRepository;
import rw.ac.auca.dropout.repository.UserRepository;
import rw.ac.auca.dropout.domain.Role;

import java.time.Instant;
import java.util.UUID;

@Service
public class InterventionService {
    private final InterventionRepository interventions;
    private final UserRepository users;

    public InterventionService(InterventionRepository interventions, UserRepository users) {
        this.interventions = interventions;
        this.users = users;
    }

    @Transactional
    public Intervention create(UUID advisorId, UUID studentId, String notes) {
        User advisor = requireRole(advisorId, Role.ADVISOR);
        User student = users.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student does not exist"));
        if (advisor.getDepartment() != student.getDepartment()) {
            throw new SecurityException("Advisors may only support students from their department");
        }
        return createForAdvisor(advisorId, studentId, notes);
    }

    @Transactional
    public Intervention assign(UUID hodId, UUID advisorId, UUID studentId, String notes) {
        var hod = requireRole(hodId, Role.HOD);
        requireRole(advisorId, Role.ADVISOR);
        var student = users.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student does not exist"));
        if (student.getDepartment() == null || hod.getDepartment() != student.getDepartment()) {
            throw new SecurityException("HODs may only assign students from their department");
        }
        var advisor = users.findById(advisorId)
                .orElseThrow(() -> new IllegalArgumentException("Advisor does not exist"));
        if (advisor.getDepartment() != hod.getDepartment()) {
            throw new SecurityException("Advisors must belong to the HOD's department");
        }
        return createForAdvisor(advisorId, studentId, notes);
    }

    private Intervention createForAdvisor(UUID advisorId, UUID studentId, String notes) {
        var student = users.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student does not exist"));
        if (!student.isAccountLocked()) {
            throw new IllegalStateException("Interventions can only be opened for a locked student");
        }
        Intervention intervention = new Intervention();
        intervention.setAdvisorId(advisorId);
        intervention.setStudentId(studentId);
        intervention.setNotes(notes.trim());
        return interventions.save(intervention);
    }

    @Transactional
    public Intervention resolve(UUID advisorId, Long interventionId, String outcome) {
        Intervention intervention = interventions.findById(interventionId)
                .orElseThrow(() -> new IllegalArgumentException("Intervention does not exist"));
        if (!intervention.getAdvisorId().equals(advisorId)) {
            throw new SecurityException("You are not assigned to this intervention");
        }
        intervention.setOutcome(outcome.trim());
        intervention.setStatus("RESOLVED");
        intervention.setResolvedAt(Instant.now());
        return interventions.save(intervention);
    }

    @Transactional
    public void unlock(UUID requesterId, UUID studentId) {
        User requester = users.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (requester.getRole() != Role.HOD && requester.getRole() != Role.ADMIN) {
            throw new SecurityException("Only Admin or HOD users may unlock accounts");
        }
        var student = users.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student does not exist"));
        if (requester.getRole() == Role.HOD
                && (requester.getDepartment() == null || requester.getDepartment() != student.getDepartment())) {
            throw new SecurityException("HODs may only unlock students from their department");
        }
        if (!interventions.existsByStudentIdAndStatus(studentId, "RESOLVED")) {
            throw new IllegalStateException("A resolved advisor intervention is required before unlocking");
        }
        student.setAccountLocked(false);
        users.save(student);
    }

    private User requireRole(UUID id, Role role) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (user.getRole() != role) throw new SecurityException("User does not have the required role");
        return user;
    }
}
