package com.example.springapp.debt;

import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        try{
            UserEntity user = userRepository.findByEmail(uName).orElseThrow();
            deb.setUser(user);
        }catch (Exception ignored){

        }
        return debtR.save(deb);
    }

    public DebtEntity debtUpdate(DebtEntity deb, Integer debtId) {
        DebtEntity debt = debtR.findById(debtId).get();
        if(!"0".equalsIgnoreCase(String.valueOf(deb.getAmount()))){
            debt.setAmount(deb.getAmount());
        }
        if(Objects.nonNull(deb.getMoneyFrom()) &&
                !"".equalsIgnoreCase(deb.getMoneyFrom())){
            debt.setMoneyFrom(deb.getMoneyFrom());
        }
        if(Objects.nonNull(deb.getStatus()) &&
                !"".equalsIgnoreCase(deb.getStatus())){
            debt.setStatus(deb.getStatus());
        }
        if(Objects.nonNull(deb.getDueDate()) &&
                !"".equalsIgnoreCase(deb.getDueDate())){
            debt.setDueDate(deb.getDueDate());
        }
        return debtR.save(debt);
    }

    public String debtDelete(Integer dId) {
        debtR.deleteById(dId);
        return "Deleted";
    }

    public List<DebtEntity> debtGet(String uName,Integer value) {
        try {
            UserEntity user = userRepository.findByEmail(uName).orElseThrow();
            if(value==1){
                return debtR.findAllByUserOrderByAmountDesc(user);
            } else if (value==2) {
                List<DebtEntity> debts = debtR.findAllByUser(user);
                return debts.stream()
                        .sorted(Comparator.comparing(debt -> parseDueDate(debt.getDueDate())))
                        .collect(Collectors.toList());
            }
            return debtR.findAllByUser(user);
        } catch (Exception e){
            return null;
        }
    }
    private Date parseDueDate(String dueDate) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
            return formatter.parse(dueDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<DebtEntity> getAllDebts() {
        return debtR.findAll();
    }

    public  DebtEntity debtGetId(Integer dId ){
        return debtR.findById(dId).get();
    }
    @Scheduled(cron = "0 30 9 * * ?")
    // Rulează zilnic la ora 9:30 AM
    public void checkDueDatesAndNotify() {
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
                sendEmail(user.getEmail(), "Debt Due Soon", "Your debt of " + debt.getAmount() + " is due on " + debt.getDueDate() + ".");
            }
        }
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void debtUpdate(Long id, String status, UserEntity user) {
        DebtEntity debt = debtR.findById(user.getUserId()).get();
        if(Objects.nonNull(status)){
            debt.setStatus(status);
        }
       debtR.save(debt);
    }
}
