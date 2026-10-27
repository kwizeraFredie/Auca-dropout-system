package rw.ac.auca.dropout.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.Intervention;
import rw.ac.auca.dropout.repository.InterventionRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interventions")
public class InterventionQueryController {
    private final InterventionRepository interventions;

    public InterventionQueryController(InterventionRepository interventions) {
        this.interventions = interventions;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ADVISOR')")
    public List<Intervention> advisorQueue(@AuthenticationPrincipal UUID advisorId) {
        return interventions.findByAdvisorIdOrderByCreatedAtDesc(advisorId);
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Intervention> studentInterventions(@AuthenticationPrincipal UUID studentId) {
        return interventions.findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}
