package com.example.springapp.budget;

import com.example.springapp.controller.BudgetController;
import com.example.springapp.user.UserEntity;

import java.util.List;
import java.util.Optional;

public interface BudgetService {

    List<Budget> getAllBudgetByUser(UserEntity user);
    Optional<Budget> getBudgetById(Long id);
    Budget createBudget(BudgetRequestDto budgetRequestDto,String userName);
    Budget updateBudget(Budget budget);

    void deleteBudget(Long id);
    boolean isBudgetExceedingLimit(Budget budget);
    boolean isMoreThanHalfSpentInFirstHalf(Budget budget);


    boolean hasAlready(String userName, int categoryId);

    Budget updateBudgetTime(Long budgetId, BudgetRequestDto budgetRequestDto);
}
