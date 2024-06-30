package com.example.springapp.transaction;

import com.example.springapp.BaseResponceDto;
import com.example.springapp.config.auth.JWTGenerator;
import com.example.springapp.transaction.pdf.TransactionImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class TransactionController {
    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionImportService transactionImportService;

    @Autowired
    JWTGenerator jwtGenerator;

    @GetMapping("/api/transactions")
    public BaseResponceDto getTransactions(@RequestHeader(value = "Authorization", defaultValue = "") String token) {
        String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));
        List<Transaction> transactions = transactionService.getTransactionsByUserName(userName);
        return new BaseResponceDto("success", transactions);
    }

    // Noua metodă de paginare
    @GetMapping("/api/transactions/paginated")
    public BaseResponceDto getTransactionsPaginated(
            @RequestHeader(value = "Authorization", defaultValue = "") String token,
            @RequestParam int page,
            @RequestParam int size) {
        String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));
        Page<Transaction> transactions = transactionService.getTransactionsByUserNameWithPagination(userName, page, size);
        return new BaseResponceDto("success", transactions.getContent());
    }


    @PostMapping("/api/transactions")
    public BaseResponceDto addTransactions(@RequestHeader(value = "Authorization", defaultValue = "") String token, @RequestBody TransactionRequestDto transactionRequestDto) {
        String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));

        Map<String, String> response = new HashMap<>();
        response = transactionService.addTransaction(transactionRequestDto, userName);
        if (response.containsKey("error")) {
            return new BaseResponceDto("error", response.get("error"));
        }
        return new BaseResponceDto("success", response.get("success"));

    }

    @PutMapping("/api/transactions")
    public BaseResponceDto updateTransactions(@RequestHeader(value = "Authorization", defaultValue = "") String token, @RequestBody TransactionRequestDto transactionRequestDto, @RequestParam String transactionId) {
        String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));
        transactionService.updateTransaction(transactionRequestDto, Integer.valueOf(transactionId), userName);
        return new BaseResponceDto("success", null);
    }

    @DeleteMapping("/api/transactions")
    public BaseResponceDto deleteTransaction(@RequestHeader(value = "Authorization", defaultValue = "") String token, @RequestParam String transactionId) {
        String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));
        System.out.println(userName);
        if (transactionService.hasTransaction(transactionId)) {
            if (transactionService.hasPermission(userName, transactionId)) {
                transactionService.deleteTransaction(Integer.parseInt(transactionId));
                return new BaseResponceDto("success");
            } else {
                return new BaseResponceDto("couldn't delete transaction");
            }
        } else {
            return new BaseResponceDto("transaction not found");
        }
    }

    @PostMapping("/api/transactions/import-pdf")
    public BaseResponceDto importTransactionsFromPdf(@RequestParam("file") MultipartFile file, @RequestHeader(value = "Authorization") String token) {
        try {
            String userName = jwtGenerator.getUsernameFromJWT(jwtGenerator.getTokenFromHeader(token));

            System.out.println("Username from token: " + userName); // Add logging for debugging
            transactionImportService.importPdf(file, userName);
            List<Transaction> transactions = transactionService.getTransactionsByUserName(userName);
            //return ResponseEntity.ok().body(Map.of("success", true, "transactions", transactions));
            return new BaseResponceDto("success", transactions);
        } catch (Exception e) {
            // Log the error for debugging purposes
            log.error("Error importing transactions from PDF", e);
            // Return a response indicating the failure
            return new BaseResponceDto("failed", e.getMessage());
        }
    }
}
