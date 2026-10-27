package rw.ac.auca.dropout.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String resetUrl;

    public PasswordResetEmailService(JavaMailSender mailSender,
                                     @Value("${app.password-reset.from}") String from,
                                     @Value("${app.password-reset.url}") String resetUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.resetUrl = resetUrl;
    }

    public void send(String recipient, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("AUCA password reset");
        message.setText("Reset your AUCA password using this link: " + resetUrl + "?token=" + token
                + "\nThis link expires in 30 minutes and can only be used once.");
        mailSender.send(message);
    }
}
