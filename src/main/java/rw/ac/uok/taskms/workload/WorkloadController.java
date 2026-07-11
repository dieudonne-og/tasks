package rw.ac.uok.taskms.workload;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Workload balance and redistribution suggestions (managers/admins). */
@RestController
@RequestMapping("/api/workload")
@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping
    public WorkloadService.WorkloadReport report() {
        return workloadService.report();
    }
}
