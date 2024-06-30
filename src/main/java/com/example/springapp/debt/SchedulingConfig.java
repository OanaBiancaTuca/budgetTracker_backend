package com.example.springapp.debt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.mail.MessagingException;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Autowired
    private DebtService debtService;

    @Scheduled(cron = "0 46 18 * * ?")
    public void scheduleDebtReset() throws MessagingException {
        debtService.resetDebts();
    }
}
