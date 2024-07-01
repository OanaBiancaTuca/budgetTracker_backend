package com.example.springapp.goals;


import com.example.springapp.user.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GoalsServiceImpl implements GoalsService {

    @Autowired
    private GoalsRepository goalsRepository;
    @Autowired
    private PredictionService predictionService;


    public Goal createGoal(Goal goal) {
        return goalsRepository.save(goal);
    }

    // Metoda updateGoal din GoalsService
    public Goal updateGoal(Long id, Goal updatedGoal) throws IOException, InterruptedException {
        Goal existingGoal = goalsRepository.findById(id).orElseThrow();
        boolean isTargetAmountChanged = existingGoal.getTargetAmount() != (updatedGoal.getTargetAmount());

        existingGoal.setName(updatedGoal.getName());
        existingGoal.setDescription(updatedGoal.getDescription());
        existingGoal.setStatus(updatedGoal.getStatus());
        existingGoal.setTargetAmount(updatedGoal.getTargetAmount());
        existingGoal.setTargetDate(updatedGoal.getTargetDate());

        Goal savedGoal = goalsRepository.save(existingGoal);

        if (isTargetAmountChanged) {
            // Recalculate predictions if target amount changed
            List<Goal> goals = this.getAllGoalsByUser(existingGoal.getUser());
            List<Goal> pendingGoals = goals.stream()
                    .filter(g -> "Pending".equalsIgnoreCase(g.getStatus()))
                    .collect(Collectors.toList());

            List<Double> times = predictionService.predictGoalAchievementTime(existingGoal.getUser().getUserId(), pendingGoals);

            if (times.size() != pendingGoals.size()) {
                System.err.println("Number of predictions does not match number of goals.");
                throw new RuntimeException("Number of predictions does not match number of goals.");
            }

            for (int i = 0; i < pendingGoals.size(); i++) {
                Goal g = pendingGoals.get(i);
                g.setPrediction((int) Math.ceil(times.get(i) * 30)); // Convert months to days
                goalsRepository.save(g);
            }
        }

        return savedGoal;
    }

    public void deleteGoal(Long id) {
        Optional<Goal> existingGoal = goalsRepository.findById(id);
        if (existingGoal.isPresent()) {
            goalsRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Goal with ID " + id + " does not exist.");
        }
    }

    public Optional<Goal> getGoal(Long id) {
        return goalsRepository.findById(id);
    }

    public List<Goal> getAllGoalsByUser(UserEntity user) {
        return goalsRepository.findAllByUser(user);
    }
}
