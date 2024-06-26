package com.example.springapp.goals;

import com.example.springapp.user.UserEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface GoalsService {
   public Goal createGoal(Goal goal);

    public Goal updateGoal(Long id, Goal goal) throws IOException, InterruptedException;

   public void deleteGoal(Long id);

   public Optional<Goal> getGoal(Long id);


    List<Goal> getAllGoalsByUser(UserEntity user);


}
