package com.example.springapp.debt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Autowired
    private DebtService debtService;
    @Scheduled(cron = "0 54 11 * * ?") // Rulează zilnic la miezul nopții
    public void scheduleDebtReset() {
        debtService.resetDebts();
    }
}
