//package com.example.springapp.pdf;
//
//import com.example.springapp.transaction.Transaction;
//import com.example.springapp.transaction.TransactionService;
//import com.example.springapp.transaction.pdf.TransactionImportService;
//import com.example.springapp.user.UserEntity;
//import com.example.springapp.user.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.util.ResourceUtils;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//class PdfImportTest {
//
//    @Autowired
//    private TransactionImportService transactionImportService;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private TransactionService transactionService;
//    private static final String TEST_USER_EMAIL = "default@outlook.com";
//
//    @BeforeEach
//    public void init(){
//        UserEntity user = new UserEntity();
//        user.setEmail(TEST_USER_EMAIL);
//        userService.register(user);
//    }
//
//    @Test
//    void nominalCase() throws IOException {
//        File file = ResourceUtils.getFile("D:\\Disertatie\\paymint-web-app-main\\paymint-web-app-main\\springapp\\src\\test\\resources\\Extras_de_cont_21591507_18052024.pdf");
//        FileInputStream input = new FileInputStream(file);
//        MockMultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "application/pdf", input);
//
//        transactionImportService.importPdf(multipartFile, TEST_USER_EMAIL);
//        List<Transaction> transactionsByUserName = transactionService.getTransactionsByUserName(TEST_USER_EMAIL);
//
//        assertThat(transactionsByUserName).isNotEmpty();
//        // Adaugă mai multe aserțiuni pentru a verifica conținutul importat, dacă este necesar
//    }
//}
