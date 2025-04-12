package com.huggingsoft.pilot_main.service.impl;

import com.huggingsoft.pilot_main.service.EmailService;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async; // Optional: for sending emails asynchronously
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Implementation of EmailService using SendGrid.
 */
@Service
public class SendGridEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final SendGrid sendGridClient;
    private final String fromEmailAddress;
    private final String fromName;

    // Constructor injection for SendGrid client and configuration
    public SendGridEmailService(
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.from-email}") String fromEmailAddress,
            @Value("${sendgrid.from-name:}") String fromName // Optional fromName
    ) {
        this.sendGridClient = new SendGrid(apiKey);
        this.fromEmailAddress = fromEmailAddress;
        this.fromName = (fromName == null || fromName.trim().isEmpty()) ? null : fromName; // Use null if name is empty
        log.info("SendGridEmailService initialized. From Email: {}", fromEmailAddress);
    }

    @Override
    @Async // Optional: Make email sending asynchronous so it doesn't block the main thread
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        String subject = "Reset Your Password - " + (fromName != null ? fromName : "Your Application");

        // --- Build HTML Content (Consider using a templating engine like Thymeleaf for complex emails) ---
        String htmlContent = String.format("""
                <html>
                <body>
                    <p>Hi %s,</p>
                    <p>You requested a password reset for your account.</p>
                    <p>Please click the link below to set a new password. This link will expire in 1 hour.</p>
                    <p><a href="%s">Reset Password</a></p>
                    <p>If you did not request a password reset, please ignore this email.</p>
                    <p>Thanks,<br/>The %s Team</p>
                </body>
                </html>
                """,
                username, // Personalize with username
                resetLink, // The secure link
                (fromName != null ? fromName : "Application") // From name
        );
        // --- You could also add a plain text version for email clients that don't support HTML ---
        // String textContent = String.format(...);

        try {
            sendEmail(toEmail, subject, htmlContent);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (IOException e) {
            log.error("Error sending password reset email to {}: {}", toEmail, e.getMessage(), e);
            // Depending on requirements, you might:
            // - Throw a custom exception to be handled upstream
            // - Queue the email for retry
            // - Just log the error (as done here)
        }
    }

    // --- Private helper to send email via SendGrid ---
    private void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        Email from = fromName != null ? new Email(fromEmailAddress, fromName) : new Email(fromEmailAddress);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent); // Use text/html for HTML emails
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        log.debug("Attempting to send email via SendGrid to: {}, Subject: {}", toEmail, subject);
        Response response = sendGridClient.api(request);
        log.debug("SendGrid Response Status Code: {}", response.getStatusCode());
        log.debug("SendGrid Response Body: {}", response.getBody()); // Log body only if needed for debugging
        log.debug("SendGrid Response Headers: {}", response.getHeaders());

        // Check if SendGrid accepted the request (usually 2xx status code means accepted for processing)
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            log.error("Failed to send email via SendGrid. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            // Throwing IOException to signal failure to the calling method
            throw new IOException("SendGrid API request failed with status code: " + response.getStatusCode());
        }
    }
}
