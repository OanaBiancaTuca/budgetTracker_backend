package com.example.springapp.debt;

import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
public class DebtService {

    private final DebtRepo debtR;
    UserRepository userRepository;
    private final JavaMailSender mailSender;

    public DebtService(DebtRepo debtR, UserRepository userRepository, JavaMailSender mailSender) {
        this.debtR = debtR;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public DebtEntity debtCreate(DebtEntity deb, String uName) {
        try {
            UserEntity user = userRepository.findByEmail(uName).orElseThrow();
            deb.setUser(user);
        } catch (Exception ignored) {
        }
        return debtR.save(deb);
    }

    public DebtEntity debtUpdate(DebtEntity deb, Integer debtId) {
        DebtEntity debt = debtR.findById(debtId).get();
        if (!"0".equalsIgnoreCase(String.valueOf(deb.getAmount()))) {
            debt.setAmount(deb.getAmount());
        }
        if (Objects.nonNull(deb.getMoneyFrom()) &&
                !"".equalsIgnoreCase(deb.getMoneyFrom())) {
            debt.setMoneyFrom(deb.getMoneyFrom());
        }
        if (Objects.nonNull(deb.getStatus()) &&
                !"".equalsIgnoreCase(deb.getStatus())) {
            debt.setStatus(deb.getStatus());
        }
        if (Objects.nonNull(deb.getDueDate()) &&
                !"".equalsIgnoreCase(deb.getDueDate())) {
            debt.setDueDate(deb.getDueDate());
        }
        return debtR.save(debt);
    }

    public String debtDelete(Integer dId) {
        debtR.deleteById(dId);
        return "Deleted";
    }

    public List<DebtEntity> debtGet(String uName, Integer value) {
        try {
            UserEntity user = userRepository.findByEmail(uName).orElseThrow();
            if (value == 1) {
                return debtR.findAllByUserOrderByAmountDesc(user);
            } else if (value == 2) {
                List<DebtEntity> debts = debtR.findAllByUser(user);
                return debts.stream()
                        .sorted(Comparator.comparing(debt -> parseDueDate(debt.getDueDate())))
                        .collect(Collectors.toList());
            }
            return debtR.findAllByUser(user);
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDueDate(String dueDate) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            return formatter.parse(dueDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public List<DebtEntity> getAllDebts() {
        return debtR.findAll();
    }

    public DebtEntity debtGetId(Integer dId) {
        return debtR.findById(dId).get();
    }

    @Scheduled(cron = "0 54 18 * * ?")
    public void checkDueDatesAndNotify() throws MessagingException, UnsupportedEncodingException {
        LocalDate today = LocalDate.now();
        LocalDate twoDaysLater = today.plusDays(2);

        List<DebtEntity> debts = debtR.findAll().stream()
                .filter(debt -> {
                    Date dueDate = parseDueDate(debt.getDueDate());
                    return dueDate != null && !dueDate.before(java.sql.Date.valueOf(today)) && !dueDate.after(java.sql.Date.valueOf(twoDaysLater));
                })
                .collect(Collectors.toList());

        for (DebtEntity debt : debts) {
            UserEntity user = debt.getUser();
            if (user != null && user.getEmail() != null) {
                Date dueDate = parseDueDate(debt.getDueDate());
                String emailContent = composeEmailContent(user.getFirstName(), debt.getMoneyFrom(), debt.getAmount(), dueDate);
                String fromAddress = "paymint.ltd@outlook.com";
                String senderName = "Paymint Team";
                String subject = "Paymint Reminder scadenta";
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message);

                helper.setFrom(fromAddress, senderName);
                helper.setTo(user.getEmail());
                helper.setSubject(subject);
                helper.setText(emailContent, true);
                mailSender.send(message);
            }
        }
    }

    private String composeEmailContent(String userName, String debtName, double amount, Date dueDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        String formattedDueDate;

        try {
            formattedDueDate = dateFormat.format(dueDate);
        } catch (IllegalArgumentException e) {
            formattedDueDate = "n/a";
        }
        return "<div>\n" +
                "    <span style=\"color:#5C6AC4;font-family: sans-serif;font-size:32px;\"><b>Paymint</b></span><br><br>\n" +
                "    <span style=\"font-family: sans-serif;\">Dragă " + userName + ",</span><br><br>\n" +
                "    <span style=\"font-family: sans-serif;\">Acesta este un reminder că scadența pentru datoria ta: <b>" + debtName + "</b>, în valoare de <b>" + amount + "</b>, este pe data de: <b>" + formattedDueDate + "</b>.</span><br><br>\n" +
                "    <span style=\"font-family: sans-serif;\">Te rugăm să efectuezi plata înainte de data scadenței pentru a evita penalizările.</span><br><br>\n" +
                "    <span style=\"font-family: sans-serif;\">Mulțumim,</span><br>\n" +
                "    <span style=\"font-family: sans-serif;\">Echipa Paymint</span>\n" +
                "</div>";
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void debtUpdate(Integer id, String status, UserEntity user) {
        DebtEntity debt = debtR.findById(id).get();
        if (Objects.nonNull(status)) {
            debt.setStatus(status);
        }
        debtR.save(debt);
    }

    public void debtUpdateDate(Integer id, String date, UserEntity user) {
        DebtEntity debt = debtR.findById(id).get();
        if (Objects.nonNull(date)) {
            debt.setDueDate(date);
        }
        debtR.save(debt);
    }

    public void resetDebts() throws MessagingException {
        List<DebtEntity> debts = debtR.findAll();
        for (DebtEntity debt : debts) {
            Date dueDate = parseDueDate(debt.getDueDate());
            LocalDate localDueDate = convertToLocalDateViaInstant(dueDate);
            if (localDueDate.isBefore(LocalDate.now())) {
                debt.resetForNextMonth();
                debtR.save(debt);
                sendEmailNotificationDebt(debt);
            }
        }
    }

    private void sendEmailNotificationDebt(DebtEntity debt) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        try {
            helper.setTo(debt.getUser().getEmail()); // Setează destinatarul emailului
            helper.setSubject("Notificare resetare scadență datorie"); // Setează subiectul emailului în română

            // Formatează data scadenței datoriei într-un format ușor de citit
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ro"));
            Date dueDate = parseDueDate(debt.getDueDate());
            LocalDate localDueDate = convertToLocalDateViaInstant(dueDate);
            String formattedDueDate = localDueDate.format(formatter);

            // Construiește conținutul emailului conform șablonului
            String emailContent = "<div>\n" +
                    "    <span style=\"color:#5C6AC4;font-family: sans-serif;font-size:32px;\"><b>Paymint</b></span><br><br>\n" +
                    "    <span style=\"font-family: sans-serif;\">Dragă " + debt.getUser().getLastName() + ",</span><br><br>\n" +
                    "    <span style=\"font-family: sans-serif;\">Acesta este un reminder că scadența pentru datoria ta: <b>" + debt.getMoneyFrom() + "</b>, în valoare de <b>" + debt.getAmount() + "</b>, a fost resetată pentru data de: <b>" + formattedDueDate + "</b>.</span><br><br>\n" +
                    "    <span style=\"font-family: sans-serif;\">Te rugăm să efectuezi plata înainte de data scadenței pentru a evita penalizările.</span><br><br>\n" +
                    "    <span style=\"font-family: sans-serif;\">Mulțumim,</span><br>\n" +
                    "    <span style=\"font-family: sans-serif;\">Echipa Paymint</span>\n" +
                    "</div>";

            helper.setText(emailContent, true); // Setează conținutul emailului ca HTML

            mailSender.send(message); // Trimite mesajul utilizând mailSender
        } catch (Exception e) {
            e.printStackTrace();
            // Tratare excepții sau notificare despre eșecul trimitere emailului
        }
    }
}
