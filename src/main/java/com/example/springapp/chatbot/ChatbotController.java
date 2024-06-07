
package com.example.springapp.chatbot;

import com.example.springapp.goals.Goal;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UserService userService;

    @PostMapping("/query")
    public ResponseEntity<String> respondToQuery(@RequestBody String query) {
        String response = chatbotService.respondToQuery(query);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/goals")
    public ResponseEntity<String> addGoal(@RequestBody Goal goal) {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        UserEntity currentUser = optionalUser.get();
        goal.setUser(currentUser);
        String response = chatbotService.addGoal(goal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/goals")
    public ResponseEntity<String> getAllGoals() {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        UserEntity currentUser = optionalUser.get();
        String response = chatbotService.getAllGoals(currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/debts/{id}/status")
    public ResponseEntity<String> updateDebtStatus(@PathVariable Long id, @RequestBody String status) {
        Optional<UserEntity> optionalUser = userService.getCurrentUser();
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        UserEntity currentUser = optionalUser.get();
        String response = chatbotService.updateDebtStatus(id, status, currentUser);
        return ResponseEntity.ok(response);
    }
}


