package rw.ac.uok.taskms.workload;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.uok.taskms.workload.WorkloadService.AssigneeLoad;
import rw.ac.uok.taskms.workload.WorkloadService.Suggestion;

import java.util.List;

@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping
    public List<AssigneeLoad> loads() {
        return workloadService.currentLoads();
    }

    @GetMapping("/suggestions")
    public List<Suggestion> suggestions() {
        return workloadService.suggestions();
    }
}
