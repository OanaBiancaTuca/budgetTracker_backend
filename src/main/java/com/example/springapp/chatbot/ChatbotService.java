package com.example.springapp.chatbot;

import com.example.springapp.chatbot.dto.ChatGPTResponse;
import com.example.springapp.debt.DebtService;
import com.example.springapp.goals.Goal;
import com.example.springapp.goals.GoalsService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class ChatbotService {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private GoalsService goalsService;

    @Autowired
    private DebtService debtService;

    @Autowired
    private UserService userService;

    private final Logger logger = Logger.getLogger(ChatbotService.class.getName());

    public String respondToQuery(String query, String sessionId) {
        return "Service temporarily unavailable. Please try again later.";
    }

    public String respondToQuery(String query) {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return "User not authenticated";
        }

        UserEntity currentUser = optionalUser.get();
        try {
            // Query AI for general responses
            ChatGPTResponse aiResponse = openAIService.getChatGPTResponse(query);

            // Logic to decide if interaction with the database is needed
            if (query.toLowerCase().contains("all goals")) {
                return getAllGoals(currentUser);
            } else if (query.toLowerCase().contains("add goal")) {
                // Extract goal details from query and add a new goal
                Goal goal = new Goal();
                goal.setName("New Goal");  // Extract name from query
                goal.setStatus("Pending"); // Extract status from query
                goal.setUser(currentUser);    // Associate the goal with the current user
                return addGoal(goal);
            } else if (query.toLowerCase().contains("update debt status")) {
                // Extract details from query and update debt status
                Long id = 1L; // Extract ID from query
                String status = "Paid"; // Extract status from query
                return updateDebtStatus(id, status, currentUser);
            }

            return String.valueOf(aiResponse.getChoices().get(0).getMessage().getContent());
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.warning("Rate limit exceeded, providing fallback response...");
            return "Service is temporarily unavailable due to high demand. Please try again later.";
        } catch (RuntimeException e) {
            // Log the error for monitoring
            logger.severe("Error generating response: " + e.getMessage());
            // Return a user-friendly message or perform other fallback actions
            return "Sorry, I'm experiencing high load right now. Please try again later.";
        }
    }

    public String getAllGoals(UserEntity user) {
        List<Goal> goals = goalsService.getAllGoalsByUser(user);
        return goals.toString(); // You can format this response to be more readable
    }

    public String addGoal(Goal goal) {
        goalsService.createGoal(goal);
        return "Goal added successfully!";
    }

    public String updateDebtStatus(Long id, String status, UserEntity user) {
        debtService.debtUpdate(id, status, user);
        return "Debt status updated successfully!";
    }
}
