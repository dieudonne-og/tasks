package rw.ac.uok.taskms.prediction;

/**
 * One labelled example for supervised learning: task attributes (features) and
 * the actual duration (label). Kept as a plain record so the prediction logic
 * can be unit-tested without the database or Weka.
 *
 * @param taskType   name of the task type (nominal feature)
 * @param assignee   stable key for the assignee, e.g. user id (nominal feature)
 * @param complexity ordinal complexity level 1..3 (numeric feature)
 * @param durationDays actual working days the task took (label)
 */
public record TrainingSample(String taskType, String assignee, int complexity, int durationDays) {
}
