package com.example.springapp.transaction;

import com.example.springapp.account.Account;
import com.example.springapp.account.AccountService;
import com.example.springapp.budget.Budget;
import com.example.springapp.budget.BudgetRepository;
import com.example.springapp.budget.BudgetService;
import com.example.springapp.category.Category;
import com.example.springapp.category.CategoryService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class TransactionService {
    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AccountService accountService;

    @Autowired
    BudgetRepository budgetRepository;

    @Autowired
    BudgetService budgetService;

    public List<Transaction> getTransactionsByUserName(String userName) {
        try {
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            List<Transaction> transactionList = transactionRepository.findAllByUser(user);
            transactionList.sort(Collections.reverseOrder());
            return transactionList;
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    public Map<String, String> addTransaction(TransactionRequestDto transactionRequestDto, String userName) {
        Map<String, String> response = new HashMap<>();
        Account account = accountService.getAccountById(transactionRequestDto.getAccountId());
        Category category = categoryService.getCategoryById(transactionRequestDto.getCategoryId());
        UserEntity user = userRepository.findByEmail(userName).orElseThrow();

        // Verificare sold negativ
        if (category.getType().equals("expense") && account.getCurrentBalance() - transactionRequestDto.getAmount() < 0) {
            response.put("error", "Sold insuficient!");
            return response;
        }

        Transaction transaction = new Transaction(
                transactionRequestDto.getAmount(),
                transactionRequestDto.getDescription(),
                transactionRequestDto.getPaymentType(),
                transactionRequestDto.getDateTime(),
                category,
                account,
                user
        );

        if (category.getType().equals("expense")) {
            List<Budget> budgets = budgetRepository.findByCategory(category);
            for (Budget budget : budgets) {
                double newUsedAmount = budget.getUsed() + transactionRequestDto.getAmount();

                // Check if the transaction exceeds the budget limit
                if (newUsedAmount > budget.getAmount()) {
                    response.put("error", "Tranzacția depășește limita bugetului");
                    return response; // Stop execution if there's an error
                }

                // Check if more than 50% of the budget is spent in the first half of the month
                if (isMoreThanHalfSpentInFirstHalf(budget, transactionRequestDto.getAmount())) {
                    response.put("error", "Mai mult de 50% din buget a fost cheltuit în prima jumătate a lunii");
                    return response; // Stop execution if there's an error
                }

                long amount = (long) (budget.getBalance() - transactionRequestDto.getAmount());
                budget.setBalance(amount);
                budget.setUsed(budget.getUsed() + (long) transactionRequestDto.getAmount());
                budgetService.updateBudget(budget);
            }

            accountService.debitBalance(account, transactionRequestDto.getAmount());
        } else if (category.getType().equals("income")) {
            accountService.creditBalance(account, transactionRequestDto.getAmount());
        }

        transactionRepository.save(transaction);
        response.put("success", "Tranzacția a fost adăugată cu succes");
        return response;
    }

    public Map<String, String> addTransaction(Transaction transaction, String userName) {
        Map<String, String> response = new HashMap<>();
        UserEntity user = userRepository.findByEmail(userName).orElseThrow();
        transaction.setUser(user);

        if (transaction.getCategory().getType().equals("expense") && transaction.getAccount().getCurrentBalance() - transaction.getAmount() < 0) {
            response.put("error", "Sold insuficient!");
            return response;
        }

        if (transaction.getCategory().getType().equals("expense")) {
            List<Budget> budgets = budgetRepository.findByCategory(transaction.getCategory());
            for (Budget budget : budgets) {
                double newUsedAmount = budget.getUsed() + transaction.getAmount();

                // Check if the transaction exceeds the budget limit
                if (newUsedAmount > budget.getAmount()) {
                    response.put("error", "Tranzacția depășește limita bugetului");
                    return response;
                }

                // Check if more than 50% of the budget is spent in the first half of the month
                if (isMoreThanHalfSpentInFirstHalf(budget, transaction.getAmount())) {
                    response.put("error", "Mai mult de 50% din buget a fost cheltuit în prima jumătate a lunii");
                    return response;
                }

                long amount = (long) (budget.getBalance() - transaction.getAmount());
                budget.setBalance(amount);
                budget.setUsed(budget.getUsed() + (long) transaction.getAmount());
                budgetService.updateBudget(budget);
            }

            accountService.debitBalance(transaction.getAccount(), transaction.getAmount());
        } else if (transaction.getCategory().getType().equals("income")) {
            accountService.creditBalance(transaction.getAccount(), transaction.getAmount());
        }

        transactionRepository.save(transaction);
        response.put("success", "Tranzacția a fost adăugată cu succes");
        return response;
    }

    private boolean isMoreThanHalfSpentInFirstHalf(Budget budget, double transactionAmount) {
        LocalDate currentDate = LocalDate.now();
        int dayOfMonth = currentDate.getDayOfMonth();
        double halfMonthLimit = budget.getAmount() / 2;
        return dayOfMonth <= 15 && (budget.getUsed() + transactionAmount) > halfMonthLimit;
    }

    public boolean hasTransaction(String transactionId) {
        try {
            Transaction entity = transactionRepository.findById(Integer.valueOf(transactionId)).orElseThrow();
            return entity.getId() == Integer.parseInt(transactionId);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean hasPermission(String userName, String transactionId) {
        try {
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            Transaction entity = transactionRepository.findById(Integer.valueOf(transactionId)).orElseThrow();
            return Objects.equals(entity.getUser().getUserId(), user.getUserId());
        } catch (Exception ignored) {
            return false;
        }
    }

    public void deleteTransaction(int id) {
        try {
            Transaction entity = transactionRepository.findById(id).orElseThrow();
            transactionRepository.delete(entity);
        } catch (Exception ignored) {
        }
    }

    public List<Transaction> getTransactionsByAccount(String userName, Account account) {
        try {
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            return transactionRepository.findAllByAccount(account);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    public void updateTransaction(TransactionRequestDto transactionRequestDto, Integer transactionId, String userName) {
        try {
            Transaction entity = transactionRepository.findById(transactionId).orElseThrow();
            Account account = accountService.getAccountById(transactionRequestDto.getAccountId());
            Category category = categoryService.getCategoryById(transactionRequestDto.getCategoryId());
            entity.setAccount(account);
            entity.setCategory(category);
            entity.setDateTime(transactionRequestDto.getDateTime());
            entity.setPaymentType(transactionRequestDto.getPaymentType());
            entity.setDescription(transactionRequestDto.getDescription());
            entity.setAmount(transactionRequestDto.getAmount());
            transactionRepository.save(entity);
        } catch (Exception ignored) {
        }
    }

    public List<Object[]> getTransactionForLastSixMonths(Integer userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -6);
        Date sixMonthsAgo = calendar.getTime();

        // Fetch expenses from the repository
        return transactionRepository.getLastSixMonthsIncome(userId, sixMonthsAgo);
    }


}
