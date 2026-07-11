package rw.ac.uok.taskms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.prediction.MlPredictionService;
import rw.ac.uok.taskms.task.*;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeds an empty database with one user per role, the HR task types, and a body of
 * synthetic completed-task history so the ML model trains and the demo works from first
 * launch. Runs only when the database is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final MlPredictionService predictionService;

    // Task type -> base duration in days.
    private static final Map<String, Double> TYPE_BASE = new java.util.LinkedHashMap<>() {{
        put("Advertise vacancy", 5.0);
        put("Shortlist candidates", 3.0);
        put("Interview candidates", 4.0);
        put("Issue offer letter", 2.0);
        put("Leave application", 1.0);
        put("Performance appraisal", 6.0);
        put("Onboarding", 3.0);
    }};

    public DataSeeder(UserRepository userRepository, TaskTypeRepository taskTypeRepository,
                      TaskRepository taskRepository, PasswordEncoder passwordEncoder,
                      MlPredictionService predictionService) {
        this.userRepository = userRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
        this.predictionService = predictionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Seeding empty database with demo data...");

        User admin = saveUser("System Administrator", "admin@uok.ac.rw", "admin123", Role.ADMIN);
        User manager = saveUser("HR Manager", "manager@uok.ac.rw", "manager123", Role.HR_MANAGER);
        User alice = saveUser("Alice Uwase", "alice@uok.ac.rw", "officer123", Role.HR_OFFICER);
        User bob = saveUser("Bob Mugisha", "bob@uok.ac.rw", "officer123", Role.HR_OFFICER);
        User claire = saveUser("Claire Ingabire", "claire@uok.ac.rw", "officer123", Role.HR_OFFICER);
        List<User> officers = List.of(alice, bob, claire);

        List<TaskType> types = new ArrayList<>();
        TYPE_BASE.forEach((name, base) -> types.add(
                taskTypeRepository.save(new TaskType(name, name + " (HR activity)"))));

        // Per-officer productivity bias (some finish faster than others).
        double[] bias = {0.9, 1.1, 1.0};
        Complexity[] complexities = Complexity.values();
        Random rnd = new Random(2026);

        // Synthetic completed history.
        for (int i = 0; i < 60; i++) {
            TaskType type = types.get(rnd.nextInt(types.size()));
            Complexity complexity = complexities[rnd.nextInt(complexities.length)];
            int officerIdx = rnd.nextInt(officers.size());
            User assignee = officers.get(officerIdx);

            double base = TYPE_BASE.get(type.getName());
            double mult = switch (complexity) {
                case LOW -> 0.8;
                case MEDIUM -> 1.0;
                case HIGH -> 1.35;
            };
            double noise = (rnd.nextDouble() - 0.5) * 1.2;
            double actual = Math.max(1, round(base * mult * bias[officerIdx] + noise));
            double estimated = Math.max(1, round(actual * 0.75)); // optimistic manual estimate

            LocalDate completed = LocalDate.now().minusDays(5 + rnd.nextInt(120));
            LocalDate start = completed.minusDays((long) Math.ceil(actual));
            LocalDate due = start.plusDays((long) Math.ceil(estimated));

            Task t = new Task();
            t.setTitle(type.getName() + " #" + (i + 1));
            t.setDescription("Historical " + type.getName().toLowerCase());
            t.setTaskType(type);
            t.setAssignee(assignee);
            t.setComplexity(complexity);
            t.setStatus(TaskStatus.DONE);
            t.setEstimatedDurationDays(estimated);
            t.setActualDurationDays(actual);
            t.setStartDate(start);
            t.setDueDate(due);
            t.setCompletedDate(completed);
            t.setCreatedBy(manager);
            taskRepository.save(t);
        }

        // A few open tasks for the board / workload demo (some deliberately tight deadlines).
        seedOpenTask(types.get(2), Complexity.HIGH, alice, manager, TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2));
        seedOpenTask(types.get(0), Complexity.MEDIUM, alice, manager, TaskStatus.TODO,
                null, LocalDate.now().plusDays(3));
        seedOpenTask(types.get(5), Complexity.HIGH, alice, manager, TaskStatus.TODO,
                null, LocalDate.now().plusDays(1));
        seedOpenTask(types.get(4), Complexity.LOW, bob, manager, TaskStatus.TODO,
                null, LocalDate.now().plusDays(10));
        seedOpenTask(types.get(1), Complexity.MEDIUM, claire, manager, TaskStatus.IN_PROGRESS,
                LocalDate.now(), LocalDate.now().plusDays(7));

        // Train the models now that history exists (PostConstruct ran before seeding).
        predictionService.train();

        // Backfill model predictions on the historical completed tasks so the dashboard
        // can compare manual-estimate accuracy against the AI model (research objective 4).
        for (Task t : taskRepository.findByStatus(TaskStatus.DONE)) {
            var p = predictionService.predict(t);
            t.setPredictedDurationDays(p.predictedDays());
            t.setPredictedLowerDays(p.lowerDays());
            t.setPredictedUpperDays(p.upperDays());
            t.setPredictionModel(p.model().name());
            taskRepository.save(t);
        }

        log.info("Seeding complete: {} users, {} task types, {} tasks",
                userRepository.count(), taskTypeRepository.count(), taskRepository.count());
    }

    private void seedOpenTask(TaskType type, Complexity complexity, User assignee, User creator,
                              TaskStatus status, LocalDate start, LocalDate due) {
        Task t = new Task();
        t.setTitle(type.getName() + " (open)");
        t.setDescription("Open " + type.getName().toLowerCase());
        t.setTaskType(type);
        t.setAssignee(assignee);
        t.setComplexity(complexity);
        t.setStatus(status);
        t.setEstimatedDurationDays(TYPE_BASE.get(type.getName()));
        t.setStartDate(start);
        t.setDueDate(due);
        t.setCreatedBy(creator);

        // Fill prediction using the fallback (models not yet trained at this point).
        var result = predictionService.predict(t);
        t.setPredictedDurationDays(result.predictedDays());
        t.setPredictedLowerDays(result.lowerDays());
        t.setPredictedUpperDays(result.upperDays());
        t.setPredictionModel(result.model().name());
        taskRepository.save(t);
    }

    private User saveUser(String fullName, String email, String rawPassword, Role role) {
        return userRepository.save(new User(fullName, email, passwordEncoder.encode(rawPassword), role));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
