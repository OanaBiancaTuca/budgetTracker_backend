package com.example.springapp.goals;

import com.example.springapp.transaction.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PredictionService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private GoalsRepository goalRepository;

    public List<Double> predictGoalAchievementTime(Integer userId, List<Goal> goals) throws IOException, InterruptedException {
        // Obține tranzacțiile pentru ultimele 6 luni
        List<Object[]> transactionsData = transactionService.getTransactionForLastSixMonths(userId);

        double totalIncome = 0;
        double totalExpenses = 0;
        int monthsWithData = transactionsData.size() > 0 ? transactionsData.size() : 1;  // pentru a preveni diviziunea cu zero

        for (Object[] data : transactionsData) {
            String type = (String) data[0];
            double total = Double.parseDouble(data[1].toString());

            if (type.equals("income")) {
                totalIncome += total;
            } else if (type.equals("expense")) {
                totalExpenses += total;
            }
        }

        double averageMonthlyIncome = totalIncome / monthsWithData;
        double averageMonthlyExpenses = totalExpenses / monthsWithData;
        double monthlySavings = averageMonthlyIncome - averageMonthlyExpenses;

        System.out.println("Average Monthly Income: " + averageMonthlyIncome);
        System.out.println("Average Monthly Expenses: " + averageMonthlyExpenses);
        System.out.println("Monthly Savings: " + monthlySavings);

        // Extrage sumele țintă din obiective
        List<Double> targetAmounts = new ArrayList<>();
        Map<Double, Goal> targetAmountToGoalMap = new HashMap<>();
        for (Goal goal : goals) {
            if ("Pending".equals(goal.getStatus())) {
                targetAmounts.add(goal.getTargetAmount());
                targetAmountToGoalMap.put(goal.getTargetAmount(), goal);
            }
        }

        // Verifică dacă nu există obiective pentru a evita diviziunea cu zero
        if (targetAmounts.isEmpty()) {
            System.err.println("No pending goals to predict for.");
            return Collections.emptyList();
        }

        // Calculează economiile lunare pe obiectiv
        double monthlySavingsPerGoal = monthlySavings / targetAmounts.size();

        // Calea absolută către scriptul Python și fișierul modelului
        String scriptPath = Paths.get("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\main\\resources\\predict.py").toAbsolutePath().toString();
        String modelPath = Paths.get("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\main\\resources\\goal_prediction_model.pkl").toAbsolutePath().toString();

        // Pregătește comanda cu toate sumele țintă
        StringBuilder commandBuilder = new StringBuilder(String.format("python %s %s %f", scriptPath, modelPath, monthlySavingsPerGoal));
        for (double targetAmount : targetAmounts) {
            commandBuilder.append(" ").append(targetAmount);
        }
        String command = commandBuilder.toString();

        // Afișează comanda construită pentru depanare
        System.out.println("Constructed Command: " + command);

        Process process = Runtime.getRuntime().exec(command);

        // Capturează ieșirea scriptului Python
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<Double> predictions = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            // Extrage doar ultima linie care conține predicțiile
            if (line.startsWith("Predictions: ")) {
                String[] predictionStrings = line.replace("Predictions: ", "").split(" ");
                for (String predictionString : predictionStrings) {
                    try {
                        predictions.add(Double.parseDouble(predictionString));
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing prediction: " + predictionString);
                    }
                }
            }
        }

        // Capturează erorile scriptului Python
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder errorBuilder = new StringBuilder();
        while ((line = errorReader.readLine()) != null) {
            errorBuilder.append(line).append("\n");
        }

        process.waitFor();

        String errors = errorBuilder.toString().trim();

        // Afișează erorile
        if (!errors.isEmpty()) {
            System.err.println("Errors from the Python script: " + errors);
        }

        // Afișează predicțiile
        System.out.println("Predictions from Python script: " + predictions);

        // Asigură-te că numărul de predicții se potrivește cu numărul de obiective în Pending
        if (predictions.size() != targetAmounts.size()) {
            throw new IllegalStateException("Number of predictions does not match number of pending goals.");
        }

        // Maparea predicțiilor la obiective folosind valoarea țintă
        for (int i = 0; i < targetAmounts.size(); i++) {
            double targetAmount = targetAmounts.get(i);
            Goal goal = targetAmountToGoalMap.get(targetAmount);
            goal.setPrediction((int) Math.ceil(predictions.get(i) * 30)); // Convert months to days
            goalRepository.save(goal);
        }

        return predictions;
    }
}
