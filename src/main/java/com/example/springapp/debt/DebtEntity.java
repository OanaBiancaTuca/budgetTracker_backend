package com.example.springapp.debt;

import com.example.springapp.user.UserEntity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
public class DebtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer debtId;

    @ManyToOne
    @JoinColumn(name = "main_user")
    private UserEntity user;

    private double amount;
    private String dueDate;
    private String moneyFrom;
    private String status;

    public Integer getDebtId() {
        return debtId;
    }

    public void setDebtId(Integer debtId) {
        this.debtId = debtId;
    }

    public UserEntity getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getMoneyFrom() {
        return moneyFrom;
    }

    public void setMoneyFrom(String moneyFrom) {
        this.moneyFrom = moneyFrom;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public DebtEntity() {
    }

    public DebtEntity(Integer debtId, UserEntity user, double amount, String dueDate, String moneyFrom, String status) {
        this.debtId = debtId;
        this.user = user;
        this.amount = amount;
        this.dueDate = dueDate;
        this.moneyFrom = moneyFrom;
        this.status = status;
    }

    public void resetForNextMonth() {
        this.status = "unpaid";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
        LocalDate localDate = LocalDate.parse(this.dueDate, formatter);
        localDate = localDate.plusMonths(1);
        this.dueDate = localDate.format(formatter);
    }

    @Override
    public String toString() {
        return "DebtEntity{" +
                "debtId=" + debtId +
                ", user=" + user +
                ", amount=" + amount +
                ", dueDate='" + dueDate + '\'' +
                ", moneyFrom='" + moneyFrom + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
