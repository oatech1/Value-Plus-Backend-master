package com.valueplus.domain.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Component
public class EmailClient {

    private final JavaMailSender emailSender;

    @Autowired
    public EmailClient(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    public void sendSimpleMessage(String to, String subject, String text)  {
        try{
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("support@valueplusagency.com");
            helper.setTo(InternetAddress.parse(to));
            helper.setSubject(subject);
            helper.setText(text, true);

            emailSender.send(message);
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    public void testSendSimpleMessage(String to, String subject, String text) throws MessagingException {

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("support@valueplusagency.com");
        helper.setTo(InternetAddress.parse(to));
        helper.setSubject(subject);
        helper.setText(text, true);

        emailSender.send(message);
    }
}
