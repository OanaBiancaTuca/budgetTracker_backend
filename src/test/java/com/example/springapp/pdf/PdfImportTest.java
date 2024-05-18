package com.example.springapp.pdf;

import com.example.springapp.transaction.Transaction;
import com.example.springapp.transaction.TransactionService;
import com.example.springapp.transaction.pdf.TransactionImportService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class PdfImportTest {

    @Autowired
    private TransactionImportService transactionImportService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;
    private static final String TEST_USER_EMAIL = "default@outlook.com";

    @BeforeEach
    public void init(){
        UserEntity user = new UserEntity();
        user.setEmail(TEST_USER_EMAIL);
        userService.register(user);
    }

    @Test
    void nominalCase() throws IOException {
        transactionImportService.importPdf("src/test/resources/Extras_de_cont_21591507_18052024.pdf", TEST_USER_EMAIL);
        List<Transaction> transactionsByUserName = transactionService.getTransactionsByUserName(TEST_USER_EMAIL);
    }
}
