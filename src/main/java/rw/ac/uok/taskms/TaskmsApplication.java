package rw.ac.uok.taskms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Task Management System with AI-Based Deadline Prediction.
 * Case study: Human Resource Department, University of Kigali.
 */
@SpringBootApplication
@EnableScheduling
public class TaskmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskmsApplication.class, args);
    }
}
