package com.huggingsoft.pilot_main.service;

/**
 * Service interface for sending emails.
 */
public interface EmailService {

    /**
     * Sends a password reset email to the specified user.
     *
     * @param toEmail   The recipient's email address.
     * @param username  The username of the recipient (for personalization).
     * @param resetLink The unique URL containing the reset token for the user to click.
     */
    void sendPasswordResetEmail(String toEmail, String username, String resetLink);

    // Add other email sending methods here as needed (e.g., welcome email, notifications)
    // void sendWelcomeEmail(String toEmail, String username);
}
