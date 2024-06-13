package com.example.springapp.chatbot;

import com.example.springapp.chatbot.dto.ChatGPTResponse;
import com.example.springapp.debt.DebtEntity;
import com.example.springapp.debt.DebtService;
import com.example.springapp.goals.Goal;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UserService userService;

    @Autowired
    private DebtService debtService;

    @Autowired
    private OpenAIService openAIService;

    private Map<String, String> commonQuestions;

    @PostConstruct
    public void init() {
        loadCommonQuestions();
    }

    private void loadCommonQuestions() {
        try (InputStreamReader reader = new InputStreamReader(new ClassPathResource("configurari_intrebari.json").getInputStream())) {
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            commonQuestions = new Gson().fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            commonQuestions = null;
        }
    }

    @PostMapping("/query")
    public ResponseEntity<String> respondToQuery(@RequestBody String query) {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        UserEntity currentUser = optionalUser.get();

        if (commonQuestions == null) {
            return ResponseEntity.status(500).body("Configurațiile întrebărilor nu au fost încărcate corect.");
        }

        String response;
        if (commonQuestions.containsKey(query.toLowerCase())) {
            response = commonQuestions.get(query.toLowerCase());
        } else {
            response = parseCommand(query, currentUser);
        }
        return ResponseEntity.ok(response);
    }

    private String parseCommand(String query, UserEntity user) {
        if (query.toLowerCase().contains("adaugă obiectivul")) {
            return parseAndAddGoal(query, user);
        } else if (query.toLowerCase().contains("vizualizează obiectivele")) {
            return chatbotService.getAllGoals(user);
        } else if (query.toLowerCase().contains("șterge obiectivul")) {
            return parseAndDeleteGoal(query);
        } else if (query.toLowerCase().contains("actualizează statutul datoriei")) {
            return parseAndUpdateDebtStatus(query, user);
        } else if (query.toLowerCase().contains("adaugă datoria")) {
            return parseAndAddDebt(query, user);
        } else if (query.toLowerCase().contains("șterge datoria")) {
            return parseAndDeleteDebt(query);
        } else if (query.toLowerCase().contains("schimbă scadența datoriei")) {
            return parseAndUpdateDebtDueDate(query, user);
        } else {
            ChatGPTResponse aiResponse = openAIService.getOpenAIResponse(query);
            if (aiResponse != null && !aiResponse.getChoices().isEmpty()) {
                return aiResponse.getChoices().get(0).getMessage().getContent();
            } else {
                return "Nu am putut genera un răspuns. Te rog să încerci din nou.";
            }
        }
    }

    private String parseAndAddGoal(String query, UserEntity user) {
        Pattern pattern = Pattern.compile("adaugă obiectivul cu numele ([^,]+), suma (\\d+) lei, targetDate (\\d{2}-\\d{2}-\\d{4})");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String amount = matcher.group(2).trim();
            String targetDateStr = matcher.group(3).trim();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            try {
                Goal goal = new Goal();
                goal.setName(name);
                goal.setDescription("Suma: " + amount + " lei, Target Date: " + targetDateStr);
                goal.setUser(user);
                goal.setTargetDate(dateFormat.parse(targetDateStr).getTime());

                return chatbotService.addGoal(goal);
            } catch (ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Adaugă obiectivul cu numele Masina, suma 1000 lei, targetDate 24-10-2024.";
        }
    }

    private String parseAndDeleteGoal(String query) {
        Pattern pattern = Pattern.compile("șterge obiectivul cu id (\\d+)");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            Long id = Long.parseLong(matcher.group(1).trim());
            return chatbotService.deleteGoal(id);
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Șterge obiectivul cu ID 1.";
        }
    }

    private String parseAndUpdateDebtStatus(String query, UserEntity user) {
        Pattern pattern = Pattern.compile("actualizează statutul datoriei cu id (\\d+) la (.+)");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            Integer id = Integer.parseInt(matcher.group(1).trim());
            String status = matcher.group(2).trim();
            return chatbotService.updateDebtStatus(id, status, user);
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Actualizează statutul datoriei cu ID 1 la achitat.";
        }
    }

    private String parseAndAddDebt(String query, UserEntity user) {
        Pattern pattern = Pattern.compile("adaugă datoria cu descrierea ([^,]+), suma (\\d+) lei, scadența (\\d{2}-\\d{2}-\\d{4})");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            String description = matcher.group(1).trim();
            String amount = matcher.group(2).trim();
            String dueDateStr = matcher.group(3).trim();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            try {
                DebtEntity debt = new DebtEntity();
                debt.setStatus(description);
                debt.setAmount(Double.parseDouble(amount));
                debt.setUser(user);
                debt.setDueDate(String.valueOf(dateFormat.parse(dueDateStr)));

                return chatbotService.addDebt(debt);
            } catch (ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Adaugă datoria cu descrierea Împrumut, suma 1000 lei, scadența 24-10-2024.";
        }
    }

    private String parseAndDeleteDebt(String query) {
        Pattern pattern = Pattern.compile("șterge datoria cu id (\\d+)");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            Integer id = Integer.parseInt(matcher.group(1).trim());
            return chatbotService.deleteDebt(id);
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Șterge datoria cu ID 1.";
        }
    }

    private String parseAndUpdateDebtDueDate(String query, UserEntity user) {
        Pattern pattern = Pattern.compile("schimbă scadența datoriei cu id (\\d+) la (\\d{2}-\\d{2}-\\d{4})");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            Integer id = Integer.parseInt(matcher.group(1).trim());
            String dueDateStr = matcher.group(2).trim();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            try {
                return chatbotService.updateDebtDueDate(id, dateFormat.parse(dueDateStr), user);
            } catch (ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        } else {
            return "Comanda nu este într-un format valid. Exemplu: Schimbă scadența datoriei cu ID 1 la 24-10-2024.";
        }
    }
}
