package com.example.transaction_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendStatementEmail(String toEmail, String userName, byte[] csvBytes) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, 
                true, 
                StandardCharsets.UTF_8.name()
        );

        
        helper.setTo(toEmail);
        helper.setSubject("Your E-Wallet Transaction Statement");
        
        String emailBody = buildEmailBody(userName);
        helper.setText(emailBody, false); 

        String fileName = "wallet-statement-" + java.time.LocalDate.now() + ".csv";
        helper.addAttachment(fileName, new ByteArrayResource(csvBytes), "text/csv");

        mailSender.send(message);
    }

    public void sendSimpleEmail(String toEmail, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, 
                false, 
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false); 

        mailSender.send(message);
    }

    private String buildEmailBody(String userName) {
        return "E-Wallet Transaction Statement\n\n" +
               "Dear " + (userName != null ? userName : "User") + ",\n\n" +
               "Thank you for using our E-Wallet service. Please find your transaction statement attached to this email.\n\n" +
               "The statement includes all your transaction history with details such as:\n" +
               "- Transaction dates and times\n" +
               "- Transaction types (Debit/Credit)\n" +
               "- Amounts\n" +
               "- Transaction status\n" +
               "- Wallet account numbers\n";
    }
}

