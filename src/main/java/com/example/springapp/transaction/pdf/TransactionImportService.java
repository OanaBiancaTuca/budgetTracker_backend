package com.example.springapp.transaction.pdf;

import com.example.springapp.account.Account;
import com.example.springapp.account.AccountRepository;
import com.example.springapp.category.Category;
import com.example.springapp.category.CategoryRepository;
import com.example.springapp.transaction.Transaction;
import com.example.springapp.transaction.TransactionService;
import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TransactionImportService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @Autowired
    private PdfRepository pdfRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    public void importPdf(MultipartFile file, String userName) throws IOException {
        InputStream inputStream = null;
        PDDocument document = null;
        try {
            inputStream = file.getInputStream();
            document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Text extracted from PDF: {}", text);

            // Save PDF to the database
            Optional<UserEntity> currentUserOpt = userService.getCurrentUser();
            if (!currentUserOpt.isPresent()) {
                log.error("User not found: {}", userName);
                return;
            }
            UserEntity currentUser = currentUserOpt.get();

            PdfEntity pdfEntity = new PdfEntity();
            pdfEntity.setUser(currentUser);
            pdfEntity.setPdfData(file.getBytes());
            pdfRepository.save(pdfEntity);

            // Get the account with name "Raiffaisen"
            Account raiffaisenAccount = accountRepository.findByNameAndUser("Raiffaisen",currentUser);
            if (raiffaisenAccount == null) {
                log.error("Account with name 'Raiffaisen' not found.");
                return;
            }

            // Extract transactions from the text
            List<Transaction> transactions = extractTransactions(text, currentUser, raiffaisenAccount);

            for (Transaction transaction : transactions) {
                if (transaction != null) {
                    log.info("Adding transaction: {}", transaction);
                    transactionService.addTransaction(transaction, userName);
                }
            }
        } finally {
            if (document != null) {
                document.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private List<Transaction> extractTransactions(String text, UserEntity currentUser, Account account) {
        List<Transaction> transactions = new ArrayList<>();

        // Split the text into lines
        String[] lines = text.split("\n");

        // Flag to indicate when we've reached the transaction section
        boolean inTransactionSection = false;

        // Regular expression patterns to match lines
        Pattern transactionPattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}\\.\\d{2}\\.\\d{4}");
        Pattern amountPattern = Pattern.compile("\\d+\\.\\d{2}");

        List<String> transactionDetails = new ArrayList<>();
        String transactionHeader = null;

        for (String line : lines) {
            if (line.startsWith("Dat`")) {
                inTransactionSection = true;
                continue;
            }
            if (!inTransactionSection) {
                continue;
            }

            Matcher transactionMatcher = transactionPattern.matcher(line);
            if (transactionMatcher.find()) {
                if (transactionHeader != null) {
                    Transaction transaction = createTransaction(transactionHeader, transactionDetails, currentUser, account);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
                transactionHeader = line;
                transactionDetails.clear();
            } else {
                transactionDetails.add(line.trim());
            }
        }

        if (transactionHeader != null) {
            Transaction transaction = createTransaction(transactionHeader, transactionDetails, currentUser, account);
            if (transaction != null) {
                transactions.add(transaction);
            }
        }

        return transactions;
    }

    private Transaction createTransaction(String transactionHeader, List<String> transactionDetails, UserEntity currentUser, Account account) {
        String[] headerParts = transactionHeader.split(" ");
        String date = headerParts[0];
        String description = String.join(" ", Arrays.copyOfRange(headerParts, 2, headerParts.length));

        Double[] amounts = extractAmounts(transactionDetails);

        String categoryName = determineCategory(description);
        Category categoryType = categoryRepository.findByName(categoryName);
        if (categoryType == null) {
            categoryType = new Category();
            categoryType.setName(categoryName);
            categoryRepository.save(categoryType);
        }

        Transaction debitTransaction = null;
        Transaction creditTransaction = null;
        if (amounts[0] != null) {
            categoryType.setType("cheltuiala");
            debitTransaction = new Transaction(amounts[0], description, "debit", convertDateToLong(date), categoryType, account, currentUser);
        }
        if (amounts[1] != null) {
            categoryType.setType("venit");
            creditTransaction = new Transaction(amounts[1], description, "credit", convertDateToLong(date), categoryType, account, currentUser);
        }

        log.info("Created transaction: {}", debitTransaction != null ? debitTransaction : creditTransaction);
        return debitTransaction != null ? debitTransaction : creditTransaction;
    }

    private Double[] extractAmounts(List<String> transactionDetails) {
        Double debit = null;
        Double credit = null;
        for (String detail : transactionDetails) {
            if (detail.matches(".*\\d+\\.\\d{2}.*")) {
                String[] parts = detail.split(" ");
                for (String part : parts) {
                    if (part.matches("\\d+\\.\\d{2}")) {
                        if (debit == null) {
                            debit = Double.parseDouble(part.replaceAll("[^0-9.]", ""));
                        } else {
                            credit = Double.parseDouble(part.replaceAll("[^0-9.]", ""));
                        }
                    }
                }
            }
        }
        return new Double[]{debit, credit};
    }

    private String determineCategory(String description) {
        description = description.toLowerCase();
        if (description.contains("rata")) {
            return "loan";
        } else if (description.contains("transfer")) {
            return "transfer";
        } else if (description.contains("plata")) {
            return "payment";
        } else if (description.contains("economii")) {
            return "savings";
        } else if (description.contains("card")) {
            return "card payment";
        } else {
            return "other";
        }
    }

    private long convertDateToLong(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate localDate = LocalDate.parse(date, formatter);
        return localDate.toEpochDay();
    }
}
