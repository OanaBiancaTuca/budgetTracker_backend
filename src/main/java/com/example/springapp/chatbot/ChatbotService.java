package com.example.springapp.chatbot;

import com.example.springapp.debt.DebtEntity;
import com.example.springapp.debt.DebtRepo;
import com.example.springapp.goals.Goal;
import com.example.springapp.goals.GoalsRepository;
import com.example.springapp.transaction.TransactionRepository;
import com.example.springapp.user.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.text.DateFormat;
import java.util.*;

@Service
public class ChatbotService {

    @Autowired
    private GoalsRepository goalsRepository;

    @Autowired
    private DebtRepo debtRepo;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${openai.api.key}")
    private String openAIKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String openAIUrl = "https://api.openai.com/v1/engines/davinci/completions";

    public String getOpenAIResponse(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAIKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", query);
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(openAIUrl, requestEntity, String.class);

        return responseEntity.getBody();
    }

    public String getAllGoals(UserEntity user) {
        List<Goal> goals = goalsRepository.findAllByUser(user);
        if (goals.isEmpty()) {
            return "Nu ai niciun obiectiv înregistrat.";
        } else {
            StringBuilder response = new StringBuilder("Obiectivele tale curente sunt:\n");
            for (Goal goal : goals) {
                response.append("- ").append(goal.getName()).append(": ").append(goal.getDescription()).append("\n");
            }
            return response.toString();
        }
    }

    public String addGoal(Goal goal) {
        goalsRepository.save(goal);
        return "Obiectivul a fost adăugat cu succes.";
    }

    public String deleteGoal(Long id) {
        goalsRepository.deleteById(id);
        return "Obiectivul a fost șters cu succes.";
    }

    public String updateDebtStatus(Integer id, String status, UserEntity user) {
        Optional<DebtEntity> debt = debtRepo.findById(id);
        if (debt.get() != null) {
            DebtEntity entity = debt.get();
            entity.setStatus(status);
            debtRepo.save(entity);
            return "Statutul datoriei a fost actualizat.";
        }
        return "Datoria nu a fost găsită.";
    }

    public String addDebt(DebtEntity debt) {
        debtRepo.save(debt);
        return "Datoria a fost adăugată cu succes.";
    }

    public String deleteDebt(Integer id) {
        debtRepo.deleteById(id);
        return "Datoria a fost ștearsă cu succes.";
    }

    public String updateDebtDueDate(Integer id, Date dueDate, UserEntity user) {
        Optional<DebtEntity> debt = debtRepo.findById(id);
        if (debt.get() != null) {
            DebtEntity entity = debt.get();
            entity.setDueDate(String.valueOf(dueDate));
            debtRepo.save(entity);
            return "Scadența datoriei a fost actualizată.";
        }
        return "Datoria nu a fost găsită.";
    }

    public String getAllDebts(UserEntity user) {
        List<DebtEntity> debts = debtRepo.findAllByUser(user);
        if (debts.isEmpty()) {
            return "Nu ai nicio datorie înregistrată.";
        } else {
            StringBuilder response = new StringBuilder("Datoriile tale curente sunt:\n");
            for (DebtEntity debt : debts) {
                response.append("- ").append(debt.getStatus()).append(": ").append(debt.getAmount()).append(" lei, Scadența: ").append(debt.getDueDate()).append("\n");
            }
            return response.toString();
        }
    }
}
