package com.example.springapp.transaction;

import com.example.springapp.account.Account;
import com.example.springapp.budget.Budget;
import com.example.springapp.budget.BudgetRepository;
import com.example.springapp.budget.BudgetService;
import com.example.springapp.category.Category;
import com.example.springapp.user.UserEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.springapp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.springapp.category.CategoryService;
import com.example.springapp.account.AccountService;

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
        try{
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            List<Transaction> transactionList = transactionRepository.findAllByUser(user);
            transactionList.sort(Collections.reverseOrder());
            return transactionList;
        }catch (UsernameNotFoundException e){
            return null;
        }
    }

    public void addTransaction(TransactionRequestDto transactionRequestDto, String userName) {
        Account account = accountService.getAccountById(transactionRequestDto.getAccountId());
        Category category = categoryService.getCategoryById(transactionRequestDto.getCategoryId());
        UserEntity user = userRepository.findByEmail(userName).orElseThrow();
        Transaction transaction = new Transaction(
                transactionRequestDto.getAmount(),
                transactionRequestDto.getDescription(),
                transactionRequestDto.getPaymentType(),
                transactionRequestDto.getDateTime(),
                category,
                account,
                user
        );
        transactionRepository.save(transaction);
        if(category.getType().equals("expense")){
            accountService.debitBalance(account,transactionRequestDto.getAmount());
            //added by me
            List<Budget> budgets = budgetRepository.findByCategory(category);
            budgets.forEach(budget->{
                long amount = (long) (budget.getBalance()-transactionRequestDto.getAmount());
                budget.setBalance(amount);
                budget.setUsed(budget.getUsed()+((long) transactionRequestDto.getAmount()));
               budgetService.updateBudget(budget);
            });

        }else if(category.getType().equals("income")) {
            accountService.creditBalance(account,transactionRequestDto.getAmount());
        }
    }

    public boolean hasTransaction(String transactionId) {
        try{
            Transaction entity= transactionRepository.findById(Integer.valueOf(transactionId)).orElseThrow();
            return entity.getId() == Integer.parseInt(transactionId);
        }catch (Exception ignored){
            return false;
        }
    }

    public boolean hasPermission(String userName, String transactionId) {
        try{
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            Transaction entity= transactionRepository.findById(Integer.valueOf(transactionId)).orElseThrow();
            return Objects.equals(entity.getUser().getUserId(), user.getUserId());
        }catch (Exception ignored){
            return false;
        }
    }

    public void deleteTransaction(int id) {
        try{
            Transaction entity= transactionRepository.findById(id).orElseThrow();
            transactionRepository.delete(entity);
        }catch (Exception ignored){
        }
    }


    public List<Transaction> getTransactionsByAccount(String userName, Account account) {
        try{
            UserEntity user = userRepository.findByEmail(userName).orElseThrow();
            return transactionRepository.findAllByAccount(account);
        }catch (UsernameNotFoundException e){
            return null;
        }
    }

    public void updateTransaction(TransactionRequestDto transactionRequestDto, Integer transactionId, String userName) {
        try{
            Transaction entity= transactionRepository.findById(transactionId).orElseThrow();
            Account account = accountService.getAccountById(transactionRequestDto.getAccountId());
            Category category = categoryService.getCategoryById(transactionRequestDto.getCategoryId());
            entity.setAccount(account);
            entity.setCategory(category);
            entity.setDateTime(transactionRequestDto.getDateTime());
            entity.setPaymentType(transactionRequestDto.getPaymentType());
            entity.setDescription(transactionRequestDto.getDescription());
            entity.setAmount(transactionRequestDto.getAmount());
            transactionRepository.save(entity);
        }catch (Exception ignored){
        }
    }
    public List<Object[]> getTransactionForLastSixMonths(Integer userId){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -6);
        Date sixMonthsAgo = calendar.getTime();

        // Fetch expenses from the repository
        return transactionRepository.getLastSixMonthsIncome(userId, sixMonthsAgo);
    }
}
