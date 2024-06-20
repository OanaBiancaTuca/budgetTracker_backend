package com.example.springapp.budget;

import com.example.springapp.category.Category;
import com.example.springapp.category.CategoryService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetServiceImpl implements BudgetService {
    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    UserRepository userRepository;

    /*public BudgetServiceImpl(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }*/

    @Override
    public List<Budget> getAllBudgetByUser(UserEntity user) {
        return budgetRepository.findAllByUser(user.getUserId());
    }

    @Override
    public Optional<Budget> getBudgetById(Long id) {
        return budgetRepository.findById(id);
    }

    @Override
    public Budget createBudget(BudgetRequestDto budgetRequestDto, String userName) {
        Category category = categoryService.getCategoryById(budgetRequestDto.getCategoryId());
        UserEntity user = userRepository.findByEmail(userName).orElseThrow();
        Budget budget = new Budget(category, budgetRequestDto.getAmount(),user,0L,(long) budgetRequestDto.getAmount());
        //modify by me
        long balance = (long) budgetRequestDto.getAmount();
        budget.setBalance(balance);
        return budgetRepository.save(budget);
    }


    @Override
    public Budget updateBudget(Budget budget) {
        budget.setBalance((long) (budget.getAmount()-budget.getUsed()));
        return budgetRepository.save(budget);
    }

    @Override
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    @Override
    public boolean hasAlready(String userName, int categoryId) {
        UserEntity user = userRepository.findByEmail(userName).orElseThrow();
        List<Budget> budgetList = budgetRepository.findAllByUser(user.getUserId());
        boolean isAlready = false;
        for (Budget b: budgetList
             ) {
            if (b.getCategory().getCategoryId() == categoryId) {
                isAlready = true;
                break;
            }
        }
        return  isAlready;
    }
    @Override
    public boolean isBudgetExceedingLimit(Budget budget) {
        return budget.getUsed() > budget.getAmount();
    }

    @Override
    public boolean isMoreThanHalfSpentInFirstHalf(Budget budget) {
        LocalDate currentDate = LocalDate.now();
        int dayOfMonth = currentDate.getDayOfMonth();
        double halfMonthLimit = budget.getAmount() / 2;
        return dayOfMonth <= 15 && budget.getUsed() > halfMonthLimit;
    }


}
