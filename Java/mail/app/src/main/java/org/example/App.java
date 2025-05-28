package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class App {
    public static void main(String[] args) {
        String username = "info@wm.no1hardy.ch";

        // Set properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "mail.no1hardy.ch");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, "");
            }
        };

        // Create a session
        Session session = Session.getInstance(props, auth);

        try {
            // Create a default MimeMessage object
            Message message = new MimeMessage(session);

            // Set From: header field
            message.setFrom(new InternetAddress(username));

            // Set To: header field
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("siha@outlook.com"));

            // Set Subject: header field
            message.setSubject("Herzlich Willkommen beim Tippspiel für die WM 2026!");

            // Set the actual message
            message.setText("Hello, this is a test email!");

            // Send message
            Transport.send(message);

            System.out.println("Email sent successfully!");

        } catch (MessagingException e) {
            System.err.println(e.getMessage());
        }
    }
}
