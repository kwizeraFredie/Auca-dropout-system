package rw.ac.auca.dropout.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.dropout.domain.Intervention;
import rw.ac.auca.dropout.service.InterventionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/interventions")
public class InterventionController {
    private final InterventionService interventionService;

    public InterventionController(InterventionService interventionService) {
        this.interventionService = interventionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADVISOR')")
    public Intervention create(
            @AuthenticationPrincipal UUID advisorId,
            @Valid @RequestBody CreateRequest request) {
        return interventionService.create(advisorId, request.studentId(), request.notes());
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('HOD')")
    public Intervention assign(
            @AuthenticationPrincipal UUID hodId,
            @Valid @RequestBody AssignRequest request) {
        return interventionService.assign(hodId, request.advisorId(), request.studentId(), request.notes());
    }

    @PutMapping("/{interventionId}/resolve")
    @PreAuthorize("hasRole('ADVISOR')")
    public Intervention resolve(
            @PathVariable Long interventionId,
            @AuthenticationPrincipal UUID advisorId,
            @Valid @RequestBody ResolveRequest request) {
        return interventionService.resolve(advisorId, interventionId, request.outcome());
    }

    @PutMapping("/students/{studentId}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    public void unlock(@AuthenticationPrincipal UUID requesterId, @PathVariable UUID studentId) {
        interventionService.unlock(requesterId, studentId);
    }

    public record CreateRequest(UUID studentId, @NotBlank @Size(max = 4000) String notes) {}
    public record AssignRequest(UUID advisorId, UUID studentId, @NotBlank @Size(max = 4000) String notes) {}
    public record ResolveRequest(@NotBlank @Size(max = 4000) String outcome) {}
}
