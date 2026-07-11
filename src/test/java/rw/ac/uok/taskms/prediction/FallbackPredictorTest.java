package rw.ac.uok.taskms.prediction;

import org.junit.jupiter.api.Test;
import rw.ac.uok.taskms.task.Complexity;
import rw.ac.uok.taskms.task.Task;
import rw.ac.uok.taskms.task.TaskRepository;
import rw.ac.uok.taskms.task.TaskStatus;
import rw.ac.uok.taskms.task.TaskType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackPredictorTest {

    private TaskType typeWithId(long id) {
        TaskType t = new TaskType("Leave application", "");
        try {
            var f = TaskType.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    private Task doneTask(TaskType type, double actual) {
        Task t = new Task();
        t.setTaskType(type);
        t.setStatus(TaskStatus.DONE);
        t.setComplexity(Complexity.MEDIUM);
        t.setActualDurationDays(actual);
        return t;
    }

    @Test
    void returnsCategoryAverageForTaskType() {
        TaskType leave = typeWithId(1L);
        TaskRepository repo = mock(TaskRepository.class);
        when(repo.findByStatus(TaskStatus.DONE)).thenReturn(List.of(
                doneTask(leave, 2.0), doneTask(leave, 4.0), doneTask(leave, 3.0)));

        FallbackPredictor predictor = new FallbackPredictor(repo);

        Task newTask = new Task();
        newTask.setTaskType(leave);
        newTask.setComplexity(Complexity.MEDIUM);

        PredictionResult result = predictor.predict(newTask);

        assertThat(result.model()).isEqualTo(ModelType.CATEGORY_AVERAGE);
        assertThat(result.predictedDays()).isEqualTo(3.0); // mean of 2,4,3
        assertThat(result.lowerDays()).isLessThanOrEqualTo(result.predictedDays());
        assertThat(result.upperDays()).isGreaterThanOrEqualTo(result.predictedDays());
    }

    @Test
    void usesGlobalAverageWhenTypeUnseen() {
        TaskType known = typeWithId(1L);
        TaskType unseen = typeWithId(99L);
        TaskRepository repo = mock(TaskRepository.class);
        when(repo.findByStatus(TaskStatus.DONE)).thenReturn(List.of(
                doneTask(known, 5.0), doneTask(known, 5.0)));

        FallbackPredictor predictor = new FallbackPredictor(repo);

        Task newTask = new Task();
        newTask.setTaskType(unseen);
        newTask.setComplexity(Complexity.LOW);

        PredictionResult result = predictor.predict(newTask);
        assertThat(result.predictedDays()).isEqualTo(5.0);
    }
}
