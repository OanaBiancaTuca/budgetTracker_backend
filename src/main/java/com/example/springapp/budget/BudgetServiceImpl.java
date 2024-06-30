package com.example.springapp.budget;

import com.example.springapp.category.Category;
import com.example.springapp.category.CategoryService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

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
        Budget budget = new Budget(category, budgetRequestDto.getAmount(), user, 0L, (long) budgetRequestDto.getAmount());
        budget.setModifiedAt(LocalDateTime.now());
        return budgetRepository.save(budget);
    }

    @Override
    public Budget updateBudget(Budget budget) {
        budget.setBalance((long) (budget.getAmount() - budget.getUsed()));
        budget.setModifiedAt(LocalDateTime.now());
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
        return budgetList.stream().anyMatch(b -> b.getCategory().getCategoryId() == categoryId);
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

    @Override
    public Budget updateBudgetTime(Long budgetId, BudgetRequestDto budgetRequestDto) {
        Budget budget = budgetRepository.findById(budgetId).orElseThrow(() -> new RuntimeException("Budget not found"));
        // Reset the budget for the current month
        budget.setAmount(budget.getInitialAmount());
        budget.setUsed(0L);
        budget.setBalance(budget.getInitialAmount());
        budget.setModifiedAt(LocalDateTime.now());

        return budgetRepository.save(budget);
    }

    @Scheduled(cron = "0 29 18 * * ?")
    public void resetMonthlyBudgets() throws MessagingException {
        List<Budget> budgets = budgetRepository.findAll();
        LocalDate currentDate = LocalDate.now();

        for (Budget budget : budgets) {
            LocalDate modifiedDate = budget.getModifiedAt().toLocalDate();
            int dayOfMonth = modifiedDate.getDayOfMonth();
            LocalDate nextResetDate = currentDate.withDayOfMonth(dayOfMonth);

            if (nextResetDate.getMonthValue() != currentDate.getMonthValue()) {
                nextResetDate = currentDate.with(TemporalAdjusters.firstDayOfNextMonth());
            }

            if (currentDate.isEqual(nextResetDate)) {
                budget.setAmount(budget.getInitialAmount());
                budget.setUsed(0L);
                budget.setBalance(budget.getInitialAmount());
                budget.setModifiedAt(LocalDateTime.now());
                budgetRepository.save(budget);
                sendEmailNotification(budget);
            }
        }
    }

    private void sendEmailNotification(Budget budget) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        try {
            helper.setTo(budget.getUser().getEmail()); // Setează destinatarul emailului
            helper.setSubject("Notificare resetare buget"); // Setează subiectul emailului în română

            // Construiește conținutul emailului
            String emailContent = String.format(
                    "<div>\n" +
                            "    <span style=\"color:#5C6AC4;font-family: sans-serif;font-size:32px;\"><b>Paymint</b></span><br><br>\n" +
                            "    <span style=\"font-family: sans-serif;\">Dragă " + budget.getUser().getFirstName() + ",</span><br><br>\n" +
                            "    <span style=\"font-family: sans-serif;\">Acesta este un reminder că bugetul tău pentru categoria <b>" + budget.getCategory().getName() + "</b> a fost resetat la suma inițială de <b>" + budget.getInitialAmount() + " " + "</b>, la data de: <b>" + budget.getModifiedAt() + "</b>.</span><br><br>\n" +
                            "    <span style=\"font-family: sans-serif;\">Te rugăm să gestionezi cheltuielile în conformitate cu noile planuri de bugetare.</span><br><br>\n" +
                            "    <span style=\"font-family: sans-serif;\">Mulțumim,</span><br>\n" +
                            "    <span style=\"font-family: sans-serif;\">Echipa Paymint</span>\n" +
                            "</div>"

            );

            helper.setText(emailContent, true); // Setează conținutul emailului ca HTML

            mailSender.send(message); // Trimite mesajul utilizând mailSender
        } catch (Exception e) {
            e.printStackTrace();
            // Tratare excepții sau notificare despre eșecul trimitere emailului
        }
    }
}

