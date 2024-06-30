package com.example.springapp.chatbot;

import com.example.springapp.chatbot.dto.ChatGPTResponse;
import com.example.springapp.debt.DebtEntity;
import com.example.springapp.debt.DebtService;
import com.example.springapp.goals.Goal;
import com.example.springapp.goals.GoalsServiceImpl;
import com.example.springapp.goals.PredictionService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UserService userService;

    @Autowired
    private DebtService debtService;

    @Autowired
    private GoalsServiceImpl goalsServiceImpl;

    @Autowired
    private OpenAIService openAIService;
    @Autowired
    PredictionService predictionService;

    private Map<String, String> commonQuestions;

    private Map<String, CommandContext> userContexts = new HashMap<>();

    @PostConstruct
    public void init() {
        loadCommonQuestions();
    }

    private void loadCommonQuestions() {
        try (InputStreamReader reader = new InputStreamReader(new ClassPathResource("configurari_intrebari.json").getInputStream())) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            commonQuestions = new Gson().fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            commonQuestions = null;
        }
    }

    @PostMapping("/query")
    public ResponseEntity<String> respondToQuery(@RequestBody Map<String, String> payload) {
        String query = payload.get("userMessage");
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        UserEntity currentUser = optionalUser.get();

        if (commonQuestions == null) {
            return ResponseEntity.status(500).body("Configurațiile întrebărilor nu au fost încărcate corect.");
        }

        String userId = currentUser.getEmail();
        CommandContext context = userContexts.getOrDefault(userId, new CommandContext());

        String response;
        try {
            if (context.isActive()) {
                response = handleStep(query, context, currentUser);
            } else {
                if (commonQuestions.containsKey(query.toLowerCase())) {
                    response = commonQuestions.get(query.toLowerCase());
                } else {
                    response = parseCommand(query, currentUser, context);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling query: {}", query, e);
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
        userContexts.put(userId, context);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debts")
    public ResponseEntity<List<DebtEntity>> getDebts() {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body(null);
        }
        UserEntity user = optionalUser.get();
        List<DebtEntity> debts = debtService.debtGet(user.getEmail(), 0);
        return ResponseEntity.ok(debts);
    }

    @GetMapping("/goals")
    public ResponseEntity<List<Goal>> getGoals() {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body(null);
        }
        UserEntity user = optionalUser.get();
        List<Goal> goals = goalsServiceImpl.getAllGoalsByUser(user);
        return ResponseEntity.ok(goals);
    }

    private String parseCommand(String query, UserEntity user, CommandContext context) {
        String command = null;
        if (query.toLowerCase().contains("adaugă obiectivul")) {
            command = "Adaugă obiectiv";
        } else if (query.toLowerCase().contains("vizualizează obiectivele")) {
            List<Goal> goals = goalsServiceImpl.getAllGoalsByUser(user);
            return formatGoals(goals);
        } else if (query.toLowerCase().contains("vizualizează datoriile")) {
            List<DebtEntity> debts = debtService.debtGet(user.getEmail(), 0);
            return formatDebts(debts);
        } else if (query.toLowerCase().contains("șterge obiectivul")) {
            command = "Șterge obiectiv";
        } else if (query.toLowerCase().contains("actualizează statusul datoriei")) {
            command = "Actualizează statusul datoriei";
        } else if (query.toLowerCase().contains("adaugă datoria")) {
            command = "Adaugă datorie";
        } else if (query.toLowerCase().contains("șterge datoria")) {
            command = "Șterge datorie";
        } else if (query.toLowerCase().contains("schimbă scadența datoriei")) {
            command = "Schimbă scadența datoriei";
        } else {
            ChatGPTResponse aiResponse = openAIService.getOpenAIResponse(query,user.getUserId());
            if (aiResponse != null && !aiResponse.getChoices().isEmpty()) {
                return aiResponse.getChoices().get(0).getMessage().getContent();
            } else {
                return "Nu am putut genera un răspuns. Te rog să încerci din nou.";
            }
        }

        if (command != null) {
            context.startNewCommand(command);
            return getOperationInstruction(command, 1);
        }
        return "Comanda nu este recunoscută.";
    }

    private String handleStep(String query, CommandContext context, UserEntity user) throws IOException, InterruptedException {
        String command = context.getCurrentCommand();
        if (command.equals("Adaugă obiectiv")) {
            return handleAddGoalStep(query, context, user);
        } else if (command.equals("Șterge obiectiv")) {
            return handleDeleteGoalStep(query, context);
        } else if (command.equals("Actualizează statusul datoriei")) {
            return handleUpdateDebtStatusStep(query, context, user);
        } else if (command.equals("Adaugă datorie")) {
            return handleAddDebtStep(query, context, user);
        } else if (command.equals("Șterge datorie")) {
            return handleDeleteDebtStep(query, context);
        } else if (command.equals("Schimbă scadența datoriei")) {
            return handleUpdateDebtDueDateStep(query, context, user);
        }
        return "Comanda nu este recunoscută.";
    }

    private String handleAddGoalStep(String query, CommandContext context, UserEntity user) throws IOException, InterruptedException {
        int step = context.getStep();
        if (step == 1) {
            context.addStepData("name", query);
            context.incrementStep();
            return getOperationInstruction("Adaugă obiectiv", step + 1);
        } else if (step == 2) {
            if (!query.matches("\\d+")) {
                return "Suma trebuie să fie un număr valid.";
            }
            context.addStepData("amount", query);
            context.incrementStep();
            return getOperationInstruction("Adaugă obiectiv", step + 1);
        } else if (step == 3) {
            context.addStepData("targetDate", query);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            try {
                Goal goal = new Goal();
                goal.setStatus("Pending");
                goal.setName(context.getStepData("name"));
                goal.setTargetAmount(Double.parseDouble(context.getStepData("amount")));
                goal.setTargetDate(dateFormat.parse(context.getStepData("targetDate")).getTime());
                goal.setUser(user);

                // Salvează noul obiectiv
                Optional<Goal> createdGoal = Optional.ofNullable(goalsServiceImpl.createGoal(goal));

                // Obține toate obiectivele utilizatorului
                List<Goal> goals = goalsServiceImpl.getAllGoalsByUser(user);

                // Filtrează obiectivele care sunt în Pending
                List<Goal> pendingGoals = goals.stream()
                        .filter(g -> "Pending".equalsIgnoreCase(g.getStatus()))
                        .collect(Collectors.toList());

                // Obține predicțiile pentru obiectivele în Pending
                List<Double> times = predictionService.predictGoalAchievementTime(goal.getUser().getUserId(), pendingGoals);

                // Verifică dacă numărul de predicții corespunde cu numărul de obiective în Pending
                if (times.size() != pendingGoals.size()) {
                    System.err.println("Number of predictions does not match number of goals.");
                    return "Eroare la calcularea predictiei";
                }

                // Actualizează obiectivele cu predicțiile calculate
                for (int i = 0; i < pendingGoals.size(); i++) {
                    Goal g = pendingGoals.get(i);
                    g.setPrediction((int) Math.ceil(times.get(i) * 30)); // Convert months to days
                    goalsServiceImpl.updateGoal(g.getId(), g);
                }

                createdGoal = goalsServiceImpl.getGoal(createdGoal.get().getId());
                String result = createdGoal != null ?
                        "Obiectiv adăugat cu succes. Pe baza cheltuielilor si veniturilor tale consider ca acesta va fi realizat in aproximativ " +
                                (createdGoal.get().getPrediction() / 30) +  // Divides days by 30 to get approximate months
                                " luni" :
                        "Eroare la adăugarea obiectivului.";

                context.reset();
                return result;
            } catch (ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        }
        return "Eroare la adăugarea obiectivului.";
    }

    private String handleDeleteGoalStep(String query, CommandContext context) {
        try {
            Long id = Long.parseLong(query.trim());
            goalsServiceImpl.deleteGoal(id);
            context.reset();
            return "Obiectiv șters cu succes.";
        } catch (NumberFormatException e) {
            return "ID-ul specificat nu este valid.";
        }
    }

    private String handleUpdateDebtStatusStep(String query, CommandContext context, UserEntity user) {
        int step = context.getStep();
        if (step == 1) {
            context.addStepData("id", query);
            context.incrementStep();
            return getOperationInstruction("Actualizează statusul datoriei", step + 1);
        } else if (step == 2) {
            context.addStepData("status", query);
            try {
                Integer id = Integer.parseInt(context.getStepData("id"));
                String status = context.getStepData("status");
                debtService.debtUpdate(id, status, user);
                context.reset();
                return "Statusul datoriei actualizat cu succes.";
            } catch (NumberFormatException e) {
                return "ID-ul specificat nu este valid.";
            }
        }
        return "Eroare la actualizarea statusului datoriei.";
    }

    private String handleAddDebtStep(String query, CommandContext context, UserEntity user) {
        int step = context.getStep();
        if (step == 1) {
            context.addStepData("description", query);
            context.incrementStep();
            return getOperationInstruction("Adaugă datorie", step + 1);
        } else if (step == 2) {
            if (!query.matches("\\d+")) {
                return "Suma trebuie să fie un număr valid.";
            }
            context.addStepData("amount", query);
            context.incrementStep();
            return getOperationInstruction("Adaugă datorie", step + 1);
        } else if (step == 3) {
            context.addStepData("moneyFrom", query);
            context.incrementStep();
            return getOperationInstruction("Adaugă datorie", step + 1);
        } else if (step == 4) {
            context.addStepData("dueDate", query);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat dbFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            try {
                DebtEntity debt = new DebtEntity();
                debt.setStatus(context.getStepData("description"));
                debt.setAmount(Double.parseDouble(context.getStepData("amount")));
                debt.setUser(user);
                debt.setMoneyFrom(context.getStepData("moneyFrom"));
                debt.setDueDate(dbFormat.format(dateFormat.parse(context.getStepData("dueDate"))));
                String result = debtService.debtCreate(debt, user.getEmail()) != null ? "Datorie adăugată cu succes." : "Eroare la adăugarea datoriei.";
                context.reset();
                return result;
            } catch (ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        }
        return "Eroare la adăugarea datoriei.";
    }

    private String handleDeleteDebtStep(String query, CommandContext context) {
        try {
            Integer id = Integer.parseInt(query.trim());
            String result = debtService.debtDelete(id);
            context.reset();
            return "Datorie ștearsă cu succes.";
        } catch (NumberFormatException e) {
            return "ID-ul specificat nu este valid.";
        }
    }

    private String handleUpdateDebtDueDateStep(String query, CommandContext context, UserEntity user) {
        int step = context.getStep();
        if (step == 1) {
            context.addStepData("id", query);
            context.incrementStep();
            return getOperationInstruction("Schimbă scadența datoriei", step + 1);
        } else if (step == 2) {
            context.addStepData("newDueDate", query);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat dbFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            try {
                Integer id = Integer.parseInt(context.getStepData("id"));
                String newDueDate = dbFormat.format(dateFormat.parse(context.getStepData("newDueDate")));
                debtService.debtUpdateDate(id, newDueDate, user);
                context.reset();
                return "Data scadenței datoriei actualizată cu succes.";
            } catch (NumberFormatException | ParseException e) {
                return "Data specificată nu este într-un format valid. Folosește formatul zz-ll-aaaa.";
            }
        }
        return "Eroare la actualizarea datei scadenței datoriei.";
    }

    private String getOperationInstruction(String operation, int step) {
        Map<String, String[]> instructions = new HashMap<>();
        instructions.put("Adaugă obiectiv", new String[]{
                "Te rog să trimiți numele obiectivului:",
                "Te rog să trimiți suma obiectivului (în lei):",
                "Te rog să trimiți data țintă (zz-ll-aaaa):"
        });
        instructions.put("Actualizează statusul datoriei", new String[]{
                "Te rog să trimiți id-ul datoriei:",
                "Te rog să trimiți noul status al datoriei:"
        });
        instructions.put("Adaugă datorie", new String[]{
                "Te rog să trimiți descrierea datoriei:",
                "Te rog să trimiți suma datoriei (în lei):",
                "Te rog să trimiți furnizorul datoriei:",
                "Te rog să trimiți data scadenței (zz-ll-aaaa):"
        });
        instructions.put("Schimbă scadența datoriei", new String[]{
                "Te rog să trimiți id-ul datoriei:",
                "Te rog să trimiți noua dată a scadenței (zz-ll-aaaa):"
        });

        String[] steps = instructions.get(operation);
        if (steps != null && step <= steps.length) {
            return steps[step - 1];
        }
        return "Comanda nu este recunoscută.";
    }

    private String formatGoals(List<Goal> goals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Obiectivele tale:\n");
        for (Goal goal : goals) {
            sb.append(String.format("ID: %d, Nume: %s, Suma: %.2f lei, Data țintă: %s\n",
                    goal.getId(), goal.getName(), goal.getTargetAmount(),
                    new SimpleDateFormat("dd-MM-yyyy").format(goal.getTargetDate())));
        }
        return sb.toString();
    }

    private String formatDebts(List<DebtEntity> debts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Datoriile tale:\n");
        for (DebtEntity debt : debts) {
            sb.append(String.format("ID: %d, Descriere: %s, Suma: %.2f lei, Data scadenței: %s\n",
                    debt.getDebtId(), debt.getStatus(), debt.getAmount(),
                    debt.getDueDate()));
        }
        return sb.toString();
    }

    private static class CommandContext {
        private String currentCommand;
        private int step;
        private Map<String, String> stepData;

        public CommandContext() {
            reset();
        }

        public void startNewCommand(String command) {
            this.currentCommand = command;
            this.step = 1;
            this.stepData = new HashMap<>();
        }

        public void addStepData(String key, String value) {
            stepData.put(key, value);
        }

        public String getStepData(String key) {
            return stepData.get(key);
        }

        public String getCurrentCommand() {
            return currentCommand;
        }

        public int getStep() {
            return step;
        }

        public void incrementStep() {
            this.step++;
        }

        public boolean isActive() {
            return currentCommand != null;
        }

        public void reset() {
            this.currentCommand = null;
            this.step = 0;
            this.stepData = null;
        }
    }
}
