package com.example.springapp.goals;

import com.example.springapp.transaction.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PredictionService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private GoalsService goalsService;

    public List<Double> predictGoalAchievementTime(Integer userId, List<Double> targetAmounts) throws IOException, InterruptedException {
        // Get transactions for the last 6 months
        List<Object[]> transactions = transactionService.getTransactionForLastSixMonths(userId);

        // Calculate cumulative income and expenses
        double cumulativeIncome = transactions.stream()
                .filter(t -> "income".equalsIgnoreCase((String) t[0]))
                .mapToDouble(t -> ((Number) t[1]).doubleValue())
                .sum();

        double cumulativeExpenses = transactions.stream()
                .filter(t -> "expense".equalsIgnoreCase((String) t[0]))
                .mapToDouble(t -> ((Number) t[1]).doubleValue())
                .sum();

        double cumulativeAmount = cumulativeIncome - cumulativeExpenses;

        System.out.println("Cumulative Income: " + cumulativeIncome);
        System.out.println("Cumulative Expenses: " + cumulativeExpenses);
        System.out.println("Cumulative Amount: " + cumulativeAmount);

        // Absolute path to the Python script and model file
        String scriptPath = Paths.get("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\main\\resources\\predict.py").toAbsolutePath().toString();
        String modelPath = Paths.get("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\main\\resources\\goal_prediction_model.pkl").toAbsolutePath().toString();

        // Prepare the command with all target amounts
        StringBuilder commandBuilder = new StringBuilder(String.format("python %s %s %f", scriptPath, modelPath, cumulativeAmount));
        for (double targetAmount : targetAmounts) {
            commandBuilder.append(" ").append(targetAmount);
        }
        String command = commandBuilder.toString();

        // Print the constructed command for debugging
        System.out.println("Constructed Command: " + command);

        Process process = Runtime.getRuntime().exec(command);

        // Capture the output of the Python script
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<Double> predictions = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            predictions.add(Double.parseDouble(line));
        }

        // Capture errors from the Python script
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder errorBuilder = new StringBuilder();
        while ((line = errorReader.readLine()) != null) {
            errorBuilder.append(line).append("\n");
        }

        process.waitFor();

        String errors = errorBuilder.toString().trim();

        // Log the errors
        if (!errors.isEmpty()) {
            System.err.println("Errors from the Python script: " + errors);
        }

        // Log the predictions
        System.out.println("Predictions from Python script: " + predictions);

        return predictions;
    }
}
