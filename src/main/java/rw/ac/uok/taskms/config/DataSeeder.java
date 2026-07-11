package rw.ac.uok.taskms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rw.ac.uok.taskms.prediction.DeadlinePredictionService;
import rw.ac.uok.taskms.task.*;
import rw.ac.uok.taskms.user.Role;
import rw.ac.uok.taskms.user.User;
import rw.ac.uok.taskms.user.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds a fresh database with the department's roles, HR task types, and a body
 * of synthetic historical (completed) tasks so the machine-learning model can
 * train and the system can be demonstrated end-to-end from first launch.
 *
 * <p>Runs only when the database is empty, so it is safe on the persistent
 * MySQL profile. Default demo credentials are printed to the log and documented
 * in the README.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeadlinePredictionService predictionService;
    private final Random random = new Random(2026);

    public DataSeeder(UserRepository userRepository,
                      TaskTypeRepository taskTypeRepository,
                      TaskRepository taskRepository,
                      PasswordEncoder passwordEncoder,
                      DeadlinePredictionService predictionService) {
        this.userRepository = userRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
        this.predictionService = predictionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already populated; skipping seed");
            return;
        }
        log.info("Seeding demo data for the HR Department task management system...");

        List<User> officers = seedUsers();
        List<TaskType> types = seedTaskTypes();
        seedCompletedHistory(officers, types);

        // Train on the synthetic history before creating open tasks so they
        // receive model-based predictions.
        predictionService.retrain();

        seedOpenTasks(officers, types);
        log.info("Seed complete. Demo login: admin@uok.ac.rw / {} (also manager@ and officer1..3@)", DEMO_PASSWORD);
    }

    private List<User> seedUsers() {
        String pw = passwordEncoder.encode(DEMO_PASSWORD);
        userRepository.save(new User("System Administrator", "admin@uok.ac.rw", pw, Role.ADMIN));
        User manager = userRepository.save(new User("Grace Umutoni (HR Manager)", "manager@uok.ac.rw", pw, Role.HR_MANAGER));
        User o1 = userRepository.save(new User("Jean Bosco (HR Officer)", "officer1@uok.ac.rw", pw, Role.HR_OFFICER));
        User o2 = userRepository.save(new User("Aline Mukamana (HR Officer)", "officer2@uok.ac.rw", pw, Role.HR_OFFICER));
        User o3 = userRepository.save(new User("Eric Niyonzima (HR Officer)", "officer3@uok.ac.rw", pw, Role.HR_OFFICER));
        // Manager and officers are the people tasks are assigned to.
        return List.of(manager, o1, o2, o3);
    }

    /** The recurring HR activities from the proposal (recruitment steps, leave, appraisal). */
    private List<TaskType> seedTaskTypes() {
        record Seed(String name, String description, int baseDays) {
        }
        List<Seed> seeds = List.of(
                new Seed("Identify vacant position", "Determine a post that is not currently occupied", 2),
                new Seed("Prepare job description", "Write duties, responsibilities and requirements", 3),
                new Seed("Advertise vacancy", "Publish the vacancy through recruitment channels", 5),
                new Seed("Review applications", "Assess received applications for eligibility", 4),
                new Seed("Shortlist candidates", "Select eligible candidates and record remarks", 3),
                new Seed("Conduct interviews", "Interview shortlisted candidates and report", 5),
                new Seed("Select and contact candidate", "Choose and notify the successful candidate", 2),
                new Seed("Issue offer and appointment letter", "Prepare and send the offer for confirmation", 3),
                new Seed("Prepare and sign contract", "Draft and execute the contract agreement", 4),
                new Seed("Induction and onboarding", "Begin employment with induction", 5),
                new Seed("Leave application processing", "Process an employee annual-leave request", 2),
                new Seed("Performance appraisal", "Conduct end-of-year performance appraisal", 6));

        List<TaskType> types = new ArrayList<>();
        for (Seed s : seeds) {
            TaskType type = taskTypeRepository.save(new TaskType(s.name(), s.description()));
            baseDurationByType.put(type.getId(), s.baseDays());
            types.add(type);
        }
        return types;
    }

    private final java.util.Map<Long, Integer> baseDurationByType = new java.util.HashMap<>();

    /**
     * Creates ~10 completed tasks per type across the officers, with durations
     * driven by type base + complexity + a small per-officer speed factor +
     * noise, so the model has a learnable signal.
     */
    private void seedCompletedHistory(List<User> people, List<TaskType> types) {
        double[] speed = {0.9, 1.0, 1.15, 0.95}; // manager, o1, o2, o3
        Complexity[] complexities = Complexity.values();
        LocalDate today = LocalDate.now();

        for (TaskType type : types) {
            int base = baseDurationByType.getOrDefault(type.getId(), 3);
            for (int i = 0; i < 10; i++) {
                int personIdx = random.nextInt(people.size());
                User assignee = people.get(personIdx);
                Complexity complexity = complexities[random.nextInt(complexities.length)];

                double complexityFactor = switch (complexity) {
                    case LOW -> 0.8;
                    case MEDIUM -> 1.0;
                    case HIGH -> 1.4;
                };
                double noise = random.nextGaussian() * 1.0;
                int actual = (int) Math.max(1, Math.round(base * complexityFactor * speed[personIdx] + noise));

                Task t = new Task();
                t.setTitle(type.getName() + " #" + (i + 1));
                t.setDescription("Historical " + type.getName().toLowerCase());
                t.setTaskType(type);
                t.setAssignee(assignee);
                t.setComplexity(complexity);
                t.setStatus(TaskStatus.DONE);
                // Manual estimate: deliberately optimistic (planning fallacy, section 2.2.2.2).
                t.setEstimatedDurationDays(Math.max(1, (int) Math.round(actual * 0.75)));
                t.setActualDurationDays(actual);

                int daysAgo = 20 + random.nextInt(300);
                LocalDate start = today.minusDays(daysAgo);
                t.setStartDate(start);
                LocalDate completed = start.plusDays(actual);
                t.setCompletedDate(completed);
                // Deadline: mostly the optimistic estimate, so many overran (realistic).
                t.setDueDate(start.plusDays(t.getEstimatedDurationDays() + random.nextInt(2)));
                t.setCreatedAt(start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
                taskRepository.save(t);
            }
        }
        log.info("Seeded {} completed tasks", taskRepository.countByStatus(TaskStatus.DONE));
    }

    /** A handful of live (open) tasks, some deliberately at risk of missing the deadline. */
    private void seedOpenTasks(List<User> people, List<TaskType> types) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            TaskType type = types.get(random.nextInt(types.size()));
            User assignee = people.get(random.nextInt(people.size()));
            Complexity complexity = Complexity.values()[random.nextInt(3)];

            Task t = new Task();
            t.setTitle(type.getName() + " (current)");
            t.setDescription("Live " + type.getName().toLowerCase());
            t.setTaskType(type);
            t.setAssignee(assignee);
            t.setComplexity(complexity);
            t.setStatus(i % 3 == 0 ? TaskStatus.IN_PROGRESS : TaskStatus.TODO);
            t.setStartDate(t.getStatus() == TaskStatus.IN_PROGRESS ? today.minusDays(1) : null);
            t.setEstimatedDurationDays(2 + random.nextInt(4));

            predictionService.applyPrediction(t);

            // Force roughly half of the tasks to be at risk by setting a tight deadline.
            double predicted = t.getPredictedDurationDays() == null ? 3 : t.getPredictedDurationDays();
            long horizon = (i % 2 == 0)
                    ? Math.max(1, Math.round(predicted) - 1)   // tight -> at risk
                    : Math.round(predicted) + 4;               // comfortable
            t.setDueDate(today.plusDays(horizon));
            taskRepository.save(t);
        }
        log.info("Seeded {} open tasks", taskRepository.findByStatusIn(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)).size());
    }
}
