package com.example.springapp.budget;


import com.example.springapp.category.Category;
import com.example.springapp.user.UserEntity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private double amount;
    private Long used;
    private Long balance;

    private Long initialAmount;
    private LocalDateTime modifiedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    public Budget() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long budgetId) {
        this.id = budgetId;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(Long initialAmount) {
        this.initialAmount = initialAmount;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Budget(Category category, double amount, UserEntity user, Long used, Long balance) {
        this.category = category;
        this.amount = amount;
        this.user = user;
        this.used = used;
        this.balance = balance;
        this.initialAmount = (long) amount;
        this.modifiedAt = LocalDateTime.now();
    }

    //Getters and Setters


    public Long getUsed() {
        return Objects.requireNonNullElse(used, 0L);
    }

    public void setUsed(Long used) {
        this.used = Objects.requireNonNullElse(used, 0L);
    }

    public Long getBalance() {
        return Objects.requireNonNullElse(balance, 0L);
    }

    public void setBalance(Long balance) {
        this.balance = Objects.requireNonNullElse(balance, 0L);

    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }


}
