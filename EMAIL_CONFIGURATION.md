# Email Configuration - SMTP Setup

## Current Configuration

The application uses Gmail SMTP for sending emails:

- **SMTP Host:** smtp.gmail.com
- **Port:** 587 (TLS)
- **Email:** noreply.skilora@gmail.com
- **Password:** skilora2001

## Features Implemented

### 1. Password Reset Email
- **Trigger:** Forgot Password View
- **Template:** HTML email with reset link
- **Expiration:** 24 hours (mentioned in email)
- **Async:** Non-blocking email sending

### 2. Application Notification Email
- **Trigger:** Employer receives new job application
- **Template:** HTML notification with candidate name and job title
- **Usage:** `EmailService.getInstance().sendApplicationNotification(email, candidate, job)`

## Usage Examples

### Send Password Reset
```java
EmailService.getInstance()
    .sendPasswordResetEmail("user@example.com")
    .thenAccept(success -> {
        if (success) {
            System.out.println("Email sent!");
        } else {
            System.err.println("Failed to send email");
        }
    });
```

### Send Application Notification
```java
EmailService.getInstance()
    .sendApplicationNotification(
        "employer@company.com",
        "Ahmed Ben Ali",
        "Senior Java Developer"
    ).thenAccept(success -> {
        // Handle result
    });
```

## Email Templates

### Password Reset Email
- **Subject:** "Réinitialisation de votre mot de passe Skilora"
- **Layout:** Header (blue) + Content + Footer
- **CTA Button:** "Réinitialiser mon mot de passe"
- **Includes:** Reset link, expiration notice (24h)

### Application Notification Email
- **Subject:** "Nouvelle candidature reçue - [Job Title]"
- **Layout:** Header (green) + Content + Footer
- **CTA Button:** "Voir la candidature"
- **Includes:** Candidate name, job title, link to employer dashboard

## Security Notes

⚠️ **Important for Production:**

1. **App Password Required:**
   - Gmail accounts with 2FA need an App Password
   - Generate at: https://myaccount.google.com/apppasswords
   - Replace `skilora2001` with the 16-character app password

2. **Environment Variables:**
   ```java
   // Move credentials to environment variables
   System.getenv("SMTP_EMAIL")
   System.getenv("SMTP_PASSWORD")
   ```

3. **Token Storage:**
   - Currently, reset tokens are generated but not stored
   - For production, save tokens to database with expiration timestamp
   - Validate token on reset page before allowing password change

4. **Rate Limiting:**
   - Implement cooldown period between reset requests
   - Prevent spam by limiting emails per IP/user

## Troubleshooting

### "Authentication Failed" Error
**Solution:** Enable "Less secure app access" OR use App Password:
1. Go to Google Account settings
2. Security → 2-Step Verification
3. App Passwords → Generate new password
4. Use the 16-character password in `FROM_PASSWORD`

### "Connection Timeout" Error
**Solution:** Check firewall/antivirus blocking port 587

### "Relay Access Denied" Error
**Solution:** Verify SMTP credentials are correct

## Testing

Run the Forgot Password flow:
1. Launch application
2. Navigate to Forgot Password view
3. Enter email address: `your-test-email@gmail.com`
4. Click "Envoyer le lien"
5. Check inbox for reset email (may take 5-10 seconds)

## Configuration Location

**File:** `src/main/java/com/skilora/services/EmailService.java`

```java
private static final String SMTP_HOST = "smtp.gmail.com";
private static final String SMTP_PORT = "587";
private static final String FROM_EMAIL = "noreply.skilora@gmail.com";
private static final String FROM_PASSWORD = "skilora2001"; // Use App Password for production
private static final String FROM_NAME = "Skilora Tunisia";
```

## Dependencies

Added to `pom.xml`:
```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

## Future Enhancements

- [ ] Email queue system for bulk sending
- [ ] Email templates with Thymeleaf/Freemarker
- [ ] Email delivery tracking and logging
- [ ] Retry mechanism for failed sends
- [ ] Support for attachments
- [ ] Multi-language templates (Arabic, English, French)
- [ ] Email preferences management
- [ ] Unsubscribe links for marketing emails

---

**Last Updated:** February 4, 2026  
**Status:** ✅ Functional - Ready for Testing
