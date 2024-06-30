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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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

    private Map<String, Set<String>> categoryKeywords = new HashMap<>();

    public TransactionImportService() {
        loadCategoryKeywords();
    }

    private void loadCategoryKeywords() {
        String[] categories = {"salariu", "transport", "cumparaturi", "facturi", "taxe"};
        for (String category : categories) {
            try {
                InputStream resource = new ClassPathResource(category + ".txt").getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
                String line;
                Set<String> keywords = new HashSet<>();
                while ((line = reader.readLine()) != null) {
                    keywords.add(line.trim().toLowerCase());
                }
                categoryKeywords.put(category, keywords);
            } catch (IOException e) {
                log.error("Failed to load keywords for category: " + category, e);
            }
        }
    }

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
                throw new IOException("User not found: " + userName);
            }
            UserEntity currentUser = currentUserOpt.get();

            PdfEntity pdfEntity = new PdfEntity();
            pdfEntity.setUser(currentUser);
            pdfEntity.setPdfData(file.getBytes());
            pdfRepository.save(pdfEntity);

            // Get the account with name "Raiffaisen"
            Account raiffaisenAccount = accountRepository.findByNameAndUser("Raiffaisen", currentUser);
            if (raiffaisenAccount == null) {
                log.info("Account with name 'Raiffaisen' not found. We will create it!");
                raiffaisenAccount = new Account();
                raiffaisenAccount.setName("Raiffaisen");
                raiffaisenAccount.setUser(currentUser);
                raiffaisenAccount.setCurrentBalance(0.0); // Inițializare sold cu 0.0
                raiffaisenAccount.setPaymentTypes(Arrays.asList("Debit Card"));
                accountRepository.save(raiffaisenAccount);
            }

            // Extract initial balance from the text
            Double initialBalance = extractInitialBalance(text);
            if (initialBalance == null) {
                log.error("Failed to extract initial balance.");
                throw new IOException("Failed to extract initial balance.");
            }

            log.info("Initial balance extracted: {}", initialBalance);

            // Reset account balance to initial balance
            raiffaisenAccount.setCurrentBalance(initialBalance);
            accountRepository.save(raiffaisenAccount);

            // Extract transactions from the text
            List<Transaction> transactions = extractTransactions(text, currentUser, raiffaisenAccount, initialBalance);

            // Process each transaction using the addTransaction method from TransactionService
            for (Transaction transaction : transactions) {
                if (transaction != null) {
                    Map<String, String> response = transactionService.addTransaction(transaction, userName);
                    if (response.containsKey("error")) {
                        throw new IOException(response.get("error"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error importing PDF", e);
            throw new IOException("Error importing PDF: " + e.getMessage());
        } finally {
            if (document != null) {
                document.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private List<Transaction> extractTransactions(String text, UserEntity currentUser, Account account, Double initialBalance) {
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
        Category categoryType = categoryRepository.findByNameAndUserId(categoryName, currentUser);
        if (categoryType == null) {
            categoryType = new Category();
            categoryType.setName(categoryName);
            categoryType.setUserId(currentUser);
            categoryRepository.save(categoryType);
        }

        Transaction debitTransaction = null;
        Transaction creditTransaction = null;
        long transactionDate = convertDateToLong(date);

        if (amounts[0] != null) {
            categoryType.setType("expense");
            debitTransaction = new Transaction(amounts[0], description, "debit", transactionDate, categoryType, account, currentUser);
        }
        if (amounts[1] != null) {
            categoryType.setType("income");
            creditTransaction = new Transaction(amounts[1], description, "credit", transactionDate, categoryType, account, currentUser);
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
        for (Map.Entry<String, Set<String>> entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (description.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "other";
    }

    private long convertDateToLong(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date, formatter);
            log.info("Parsed date: {}", localDate);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date: {}. Setting to default date.", date);
            localDate = LocalDate.of(2000, 1, 1); // Default date
        }

        LocalDateTime localDateTime = localDate.atTime(0, 0); // Adăugăm ora și minutele (00:00)

        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Double extractInitialBalance(String text) {
        Pattern initialBalancePattern = Pattern.compile("Sold final\\s+(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)");
        Matcher matcher = initialBalancePattern.matcher(text);

        if (matcher.find()) {
            String balanceString = matcher.group(1).replaceAll(",", ""); // Îndepărtăm virgula din număr
            try {
                return Double.parseDouble(balanceString.trim());
            } catch (NumberFormatException e) {
                log.error("Failed to parse initial balance: {}", balanceString);
            }
        } else {
            log.error("Initial balance not found in text.");
        }

        return null;
    }
}
