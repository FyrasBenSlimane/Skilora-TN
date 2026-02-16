# MODULE 4: Support & Assistant Intelligent — Agent Prompt

> **Goal:** Implement the complete Support & Assistant Intelligent module from scratch. This module handles support tickets, ticket messaging, chatbot conversations, FAQ articles, auto-responses, and user feedback. Build everything: entities, services, controllers, views, DB schema, and i18n.

---

## CONTEXT — READ FIRST

### Project Architecture

- **Language:** Java 17, JavaFX 21, Maven
- **Database:** MySQL 8.0 via HikariCP connection pool
- **Pattern:** MVC (Model = entity + service, View = FXML, Controller = JavaFX controller)
- **NO DAO LAYER.** Services directly use `DatabaseConfig.getInstance().getConnection()` with PreparedStatement
- **Singleton pattern** on all services: `private static volatile XxxService instance; public static XxxService getInstance()`
- **I18n:** All UI strings use `I18n.get("key")` from `com.skilora.utils.I18n`. Resource bundles at `src/main/resources/com/skilora/i18n/messages_xx.properties`
- **Logging:** SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **UI Components:** Custom TL* components from `com.skilora.framework.components` (TLButton, TLCard, TLBadge, TLTextField, TLSelect, TLAlert, TLDialog, TLTextarea, TLAccordion, etc.)
- **Async:** All DB calls from controllers use `javafx.concurrent.Task` with `Platform.runLater()` for UI updates

### Key File Locations

```
src/main/java/com/skilora/
├── config/DatabaseConfig.java          — DB connection singleton (HikariCP)
├── config/DatabaseInitializer.java     — Auto-creates tables on startup
├── model/entity/                       — Pure POJO entities (no JavaFX deps)
├── model/enums/                        — Enum types
├── model/service/                      — Service singletons (CRUD + business logic)
├── controller/                         — FXML controllers
├── ui/MainView.java                    — Main navigation (loads FXML views)
├── utils/I18n.java                     — Internationalization utility

src/main/resources/com/skilora/
├── i18n/messages_*.properties          — 4 resource bundles (default/fr/en/ar)
├── view/*.fxml                         — FXML view files
```

### Existing Patterns to Follow

**Entity pattern:** Pure POJO in `com.skilora.model.entity` — no JavaFX imports, private fields, constructors, getters/setters, equals/hashCode/toString.

**Service pattern:** Singleton in `com.skilora.model.service` with direct JDBC. Use `DatabaseConfig.getInstance().getConnection()`. All CRUD via PreparedStatement. Private `mapResultSet(ResultSet rs)` method.

**Controller pattern:** Implements `Initializable`, uses `@FXML` annotations, `Task` for async DB calls, `I18n.get()` for all text, `logger` for logging. `setCurrentUser(User user)` to receive logged-in user from MainView.

**FXML pattern:** Uses TL* components, VBox root with padding/spacing, header with title + stats + action buttons, filter area, content area (VBox or FlowPane), empty state VBox.

**DatabaseInitializer pattern:** `if (!tableExists(stmt, "table_name")) { createXxxTable(stmt); }` with SQL in text blocks.

**MainView navigation pattern:**
```java
private void showXxxView() {
    centerStack.getChildren().clear();
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/XxxView.fxml"));
        VBox content = loader.load();
        XxxController controller = loader.getController();
        if (controller != null) { controller.setCurrentUser(currentUser); }
        TLScrollArea scrollArea = new TLScrollArea(content);
        scrollArea.setFitToWidth(true);
        scrollArea.setFitToHeight(true);
        scrollArea.getStyleClass().add("transparent-bg");
        centerStack.getChildren().add(scrollArea);
        animateEntry(content, 0);
    } catch (Exception e) { logger.error("Failed to load XxxView", e); }
}
```

---

## WHAT EXISTS NOW

**Nothing.** No entities, services, controllers, or views exist for the Support module. Everything must be built from scratch.

---

## PHASE 1: Database Schema

### Step 1.1 — Add all tables to `DatabaseInitializer.java`

Add these table creation methods following the existing pattern:

```sql
CREATE TABLE IF NOT EXISTS support_tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'OPEN',
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to INT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_date DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS ticket_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    sender_id INT NOT NULL,
    message TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS faq_articles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    language VARCHAR(5) DEFAULT 'fr',
    helpful_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    is_published BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chatbot_conversations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    started_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_date DATETIME,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    escalated_to_ticket_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (escalated_to_ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS chatbot_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    intent VARCHAR(100),
    confidence DECIMAL(5,4),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES chatbot_conversations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auto_responses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trigger_keyword VARCHAR(100) NOT NULL,
    response_text TEXT NOT NULL,
    category VARCHAR(50),
    language VARCHAR(5) DEFAULT 'fr',
    is_active BOOLEAN DEFAULT TRUE,
    usage_count INT DEFAULT 0,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    rating INT DEFAULT 0,
    comment TEXT,
    category VARCHAR(50),
    is_resolved BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Add indexes:
```sql
CREATE INDEX idx_tickets_user ON support_tickets(user_id);
CREATE INDEX idx_tickets_status ON support_tickets(status);
CREATE INDEX idx_tickets_priority ON support_tickets(priority);
CREATE INDEX idx_tickets_assigned ON support_tickets(assigned_to);
CREATE INDEX idx_ticket_messages_ticket ON ticket_messages(ticket_id);
CREATE INDEX idx_chatbot_conv_user ON chatbot_conversations(user_id);
CREATE INDEX idx_chatbot_msg_conv ON chatbot_messages(conversation_id);
CREATE INDEX idx_faq_category ON faq_articles(category);
CREATE INDEX idx_feedback_user ON user_feedback(user_id);
```

---

## PHASE 2: Enums

### Step 2.1 — Create `TicketPriority.java`

File: `src/main/java/com/skilora/model/enums/TicketPriority.java`

```java
package com.skilora.model.enums;

public enum TicketPriority {
    LOW, MEDIUM, HIGH, URGENT
}
```

### Step 2.2 — Create `TicketStatus.java`

File: `src/main/java/com/skilora/model/enums/TicketStatus.java`

```java
package com.skilora.model.enums;

public enum TicketStatus {
    OPEN, IN_PROGRESS, WAITING_REPLY, RESOLVED, CLOSED
}
```

### Step 2.3 — Create `FeedbackType.java`

File: `src/main/java/com/skilora/model/enums/FeedbackType.java`

```java
package com.skilora.model.enums;

public enum FeedbackType {
    BUG_REPORT, FEATURE_REQUEST, GENERAL, COMPLAINT, PRAISE
}
```

---

## PHASE 3: Entities

### Step 3.1 — Create `SupportTicket.java`

File: `src/main/java/com/skilora/model/entity/SupportTicket.java`

Fields: `id`, `userId`, `category` (String), `priority` (String — maps to TicketPriority), `status` (String — maps to TicketStatus), `subject`, `description`, `assignedTo` (Integer — nullable), `createdDate` (LocalDateTime), `updatedDate` (LocalDateTime), `resolvedDate` (LocalDateTime — nullable).

Transient fields: `userName`, `assignedToName`.

### Step 3.2 — Create `TicketMessage.java`

File: `src/main/java/com/skilora/model/entity/TicketMessage.java`

Fields: `id`, `ticketId`, `senderId`, `message`, `isInternal` (boolean), `createdDate` (LocalDateTime).

Transient: `senderName`, `senderRole`.

### Step 3.3 — Create `FAQArticle.java`

File: `src/main/java/com/skilora/model/entity/FAQArticle.java`

Fields: `id`, `category`, `question`, `answer`, `language`, `helpfulCount`, `viewCount`, `isPublished` (boolean), `createdDate`, `updatedDate`.

### Step 3.4 — Create `ChatbotConversation.java`

File: `src/main/java/com/skilora/model/entity/ChatbotConversation.java`

Fields: `id`, `userId`, `startedDate`, `endedDate`, `status`, `escalatedToTicketId` (Integer — nullable).

### Step 3.5 — Create `ChatbotMessage.java`

File: `src/main/java/com/skilora/model/entity/ChatbotMessage.java`

Fields: `id`, `conversationId`, `sender` (String: "USER" or "BOT"), `message`, `intent`, `confidence` (double), `createdDate`.

### Step 3.6 — Create `AutoResponse.java`

File: `src/main/java/com/skilora/model/entity/AutoResponse.java`

Fields: `id`, `triggerKeyword`, `responseText`, `category`, `language`, `isActive` (boolean), `usageCount`, `createdDate`.

### Step 3.7 — Create `UserFeedback.java`

File: `src/main/java/com/skilora/model/entity/UserFeedback.java`

Fields: `id`, `userId`, `feedbackType` (String), `rating` (int, 1-5), `comment`, `category`, `isResolved` (boolean), `createdDate`.

Transient: `userName`.

---

## PHASE 4: Services

### Step 4.1 — Create `SupportTicketService.java`

File: `src/main/java/com/skilora/model/service/SupportTicketService.java`

Singleton with CRUD:
- `int create(SupportTicket ticket)` — INSERT
- `SupportTicket findById(int id)` — SELECT with JOIN users for names
- `List<SupportTicket> findByUserId(int userId)` — user's tickets
- `List<SupportTicket> findByAssignedTo(int adminId)` — admin's assigned tickets
- `List<SupportTicket> findAll()` — all tickets (admin view)
- `List<SupportTicket> findByStatus(String status)` — filtered
- `List<SupportTicket> findByPriority(String priority)` — filtered
- `boolean update(SupportTicket ticket)` — UPDATE
- `boolean updateStatus(int id, String status)` — change status, set resolvedDate if RESOLVED
- `boolean assign(int ticketId, int adminId)` — UPDATE assigned_to
- `boolean delete(int id)` — DELETE
- `long countByStatus(String status)` — SELECT COUNT
- `long countOpen()` — count OPEN + IN_PROGRESS
- Private `SupportTicket mapResultSet(ResultSet rs)`

### Step 4.2 — Create `TicketMessageService.java`

File: `src/main/java/com/skilora/model/service/TicketMessageService.java`

- `int addMessage(TicketMessage msg)` — INSERT + update ticket's updatedDate
- `List<TicketMessage> findByTicketId(int ticketId)` — ORDER BY created_date ASC, JOIN users for senderName
- `boolean delete(int id)` — DELETE

### Step 4.3 — Create `FAQService.java`

File: `src/main/java/com/skilora/model/service/FAQService.java`

- `int create(FAQArticle article)` — INSERT
- `FAQArticle findById(int id)` — SELECT
- `List<FAQArticle> findAll()` — published only
- `List<FAQArticle> findByCategory(String category)`
- `List<FAQArticle> search(String query)` — LIKE on question + answer
- `List<FAQArticle> findByLanguage(String language)`
- `boolean update(FAQArticle article)` — UPDATE
- `boolean delete(int id)` — DELETE
- `boolean incrementHelpful(int id)` — UPDATE helpful_count = helpful_count + 1
- `boolean incrementViews(int id)` — UPDATE view_count = view_count + 1

### Step 4.4 — Create `ChatbotService.java`

File: `src/main/java/com/skilora/model/service/ChatbotService.java`

- `int startConversation(int userId)` — INSERT new conversation
- `ChatbotConversation findById(int id)` — SELECT
- `List<ChatbotConversation> findByUserId(int userId)` — user's conversations
- `boolean endConversation(int id)` — UPDATE status = 'ENDED', endedDate = NOW()
- `int addMessage(ChatbotMessage msg)` — INSERT
- `List<ChatbotMessage> getMessages(int conversationId)` — ORDER BY created_date ASC
- `boolean escalateToTicket(int conversationId, int ticketId)` — UPDATE escalated_to_ticket_id
- `String getAutoResponse(String userMessage)` — search auto_responses for matching keywords, return response_text, increment usage_count. If no match, return null.

### Step 4.5 — Create `AutoResponseService.java`

File: `src/main/java/com/skilora/model/service/AutoResponseService.java`

Admin CRUD:
- `int create(AutoResponse ar)` — INSERT
- `List<AutoResponse> findAll()` — all responses
- `List<AutoResponse> findActive()` — WHERE is_active = TRUE
- `boolean update(AutoResponse ar)` — UPDATE
- `boolean delete(int id)` — DELETE
- `boolean toggleActive(int id)` — flip is_active

### Step 4.6 — Create `UserFeedbackService.java`

File: `src/main/java/com/skilora/model/service/UserFeedbackService.java`

- `int submit(UserFeedback fb)` — INSERT
- `List<UserFeedback> findByUserId(int userId)` — user's feedback
- `List<UserFeedback> findAll()` — admin view, JOIN users
- `List<UserFeedback> findByType(String type)` — filtered
- `boolean resolve(int id)` — UPDATE is_resolved = TRUE
- `boolean update(UserFeedback fb)` — UPDATE
- `boolean delete(int id)` — DELETE
- `double getAverageRating()` — SELECT AVG(rating)

---

## PHASE 5: Controllers & Views

### Step 5.1 — Create `SupportController.java`

File: `src/main/java/com/skilora/controller/SupportController.java`

Main support view controller:
- Tabbed interface: "My Tickets" | "FAQ" | "Chatbot" | "Feedback"
- **My Tickets tab:**
  - List user's tickets as TLCards with status badges
  - "New Ticket" button → dialog with subject, category select, priority select, description textarea
  - Click ticket → opens ticket detail with message thread
  - Reply to ticket (add TicketMessage)
  - Close/Reopen ticket
- **FAQ tab:**
  - Searchable list of FAQ articles using TLAccordion or expandable cards
  - "Was this helpful?" buttons
  - Category filter
- **Chatbot tab:**
  - Chat-style UI: messages in a scrollable VBox
  - Text input + send button
  - Bot responses from auto_responses or "I'll escalate this" if no match → creates a ticket
- **Feedback tab:**
  - Submit feedback form: type select, rating (1-5), comment textarea
  - History of user's submitted feedback

### Step 5.2 — Create `SupportView.fxml`

File: `src/main/resources/com/skilora/view/SupportView.fxml`

Layout:
```xml
<VBox spacing="24" style="-fx-background-color: transparent;">
    <padding><Insets top="32" right="32" bottom="32" left="32"/></padding>

    <!-- Header -->
    <HBox spacing="16" alignment="CENTER_LEFT">
        <VBox spacing="4">
            <Label fx:id="titleLabel" text="Support" styleClass="h2"/>
            <Label fx:id="subtitleLabel" styleClass="text-muted"/>
        </VBox>
        <Region HBox.hgrow="ALWAYS"/>
        <TLButton fx:id="newTicketBtn" text="Nouveau Ticket" variant="PRIMARY" onAction="#handleNewTicket"/>
    </HBox>

    <!-- Tab Navigation -->
    <HBox fx:id="tabBox" spacing="8" alignment="CENTER_LEFT"/>

    <!-- Content Pane (switches based on tab) -->
    <VBox fx:id="contentPane" VBox.vgrow="ALWAYS"/>
</VBox>
```

### Step 5.3 — Create `SupportAdminController.java` (Admin view)

File: `src/main/java/com/skilora/controller/SupportAdminController.java`

Admin-only view for managing all tickets:
- See all tickets with filters (status, priority, category)
- Assign tickets to admins
- View ticket details + internal notes
- Manage FAQ articles (CRUD)
- Manage auto-responses (CRUD)
- View all user feedback
- Stats: open tickets count, avg response time, avg rating

### Step 5.4 — Create `SupportAdminView.fxml`

Similar layout to SupportView but with admin-specific tabs: "All Tickets" | "FAQ Management" | "Auto-Responses" | "Feedback" | "Stats"

---

## PHASE 6: Navigation Integration

### Step 6.1 — Add to `MainView.java`

Add navigation entries for Support. Add a `showSupportView()` method and a nav button:

1. Add a sidebar nav button: `createNavButton(I18n.get("nav.support"), SVG_SUPPORT_ICON, this::showSupportView)`
2. Implement `showSupportView()` following the standard pattern
3. For admin users, show the admin version (`SupportAdminView.fxml`)
4. Add cached view field: `private javafx.scene.Node cachedSupportView;`

Use this SVG for the support icon:
```
M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z
```

---

## PHASE 7: I18n Keys

### Step 7.1 — Add keys to all 4 resource bundles

**French (messages.properties / messages_fr.properties):**
```properties
# Support Module
nav.support=Support
support.title=Centre d'aide
support.subtitle=Comment pouvons-nous vous aider ?
support.tab.tickets=Mes Tickets
support.tab.faq=FAQ
support.tab.chatbot=Assistant
support.tab.feedback=Feedback

# Tickets
ticket.new=Nouveau Ticket
ticket.subject=Sujet
ticket.description=Description
ticket.category=Catégorie
ticket.category.general=Général
ticket.category.technical=Technique
ticket.category.billing=Facturation
ticket.category.account=Compte
ticket.category.job=Emploi
ticket.category.formation=Formation
ticket.priority=Priorité
ticket.priority.low=Basse
ticket.priority.medium=Moyenne
ticket.priority.high=Haute
ticket.priority.urgent=Urgente
ticket.status=Statut
ticket.status.open=Ouvert
ticket.status.in_progress=En cours
ticket.status.waiting_reply=En attente de réponse
ticket.status.resolved=Résolu
ticket.status.closed=Fermé
ticket.reply=Répondre
ticket.close=Fermer le ticket
ticket.reopen=Rouvrir le ticket
ticket.assign=Assigner
ticket.empty=Aucun ticket
ticket.count={0} tickets
ticket.created=Ticket créé avec succès
ticket.updated=Ticket mis à jour
ticket.deleted=Ticket supprimé

# FAQ
faq.title=Questions fréquentes
faq.search=Rechercher dans la FAQ...
faq.helpful=Utile ?
faq.yes=Oui
faq.no=Non
faq.empty=Aucun article trouvé
faq.new=Nouvel article
faq.edit=Modifier
faq.delete=Supprimer

# Chatbot
chatbot.title=Assistant virtuel
chatbot.welcome=Bonjour ! Comment puis-je vous aider ?
chatbot.placeholder=Tapez votre message...
chatbot.send=Envoyer
chatbot.escalate=Transférer à un agent
chatbot.no_match=Je ne suis pas sûr de comprendre. Voulez-vous créer un ticket de support ?

# Feedback
feedback.title=Votre avis
feedback.type=Type
feedback.type.bug=Signaler un bug
feedback.type.feature=Suggestion
feedback.type.general=Général
feedback.type.complaint=Réclamation
feedback.type.praise=Compliment
feedback.rating=Note
feedback.comment=Commentaire
feedback.submit=Envoyer
feedback.submitted=Merci pour votre feedback !
feedback.empty=Aucun feedback
```

**English (messages_en.properties):**
```properties
nav.support=Support
support.title=Help Center
support.subtitle=How can we help you?
support.tab.tickets=My Tickets
support.tab.faq=FAQ
support.tab.chatbot=Assistant
support.tab.feedback=Feedback

ticket.new=New Ticket
ticket.subject=Subject
ticket.description=Description
ticket.category=Category
ticket.category.general=General
ticket.category.technical=Technical
ticket.category.billing=Billing
ticket.category.account=Account
ticket.category.job=Job
ticket.category.formation=Training
ticket.priority=Priority
ticket.priority.low=Low
ticket.priority.medium=Medium
ticket.priority.high=High
ticket.priority.urgent=Urgent
ticket.status=Status
ticket.status.open=Open
ticket.status.in_progress=In Progress
ticket.status.waiting_reply=Waiting Reply
ticket.status.resolved=Resolved
ticket.status.closed=Closed
ticket.reply=Reply
ticket.close=Close Ticket
ticket.reopen=Reopen Ticket
ticket.assign=Assign
ticket.empty=No tickets
ticket.count={0} tickets
ticket.created=Ticket created successfully
ticket.updated=Ticket updated
ticket.deleted=Ticket deleted

faq.title=Frequently Asked Questions
faq.search=Search FAQ...
faq.helpful=Helpful?
faq.yes=Yes
faq.no=No
faq.empty=No articles found
faq.new=New Article
faq.edit=Edit
faq.delete=Delete

chatbot.title=Virtual Assistant
chatbot.welcome=Hello! How can I help you?
chatbot.placeholder=Type your message...
chatbot.send=Send
chatbot.escalate=Transfer to agent
chatbot.no_match=I'm not sure I understand. Would you like to create a support ticket?

feedback.title=Your Feedback
feedback.type=Type
feedback.type.bug=Bug Report
feedback.type.feature=Feature Request
feedback.type.general=General
feedback.type.complaint=Complaint
feedback.type.praise=Praise
feedback.rating=Rating
feedback.comment=Comment
feedback.submit=Submit
feedback.submitted=Thank you for your feedback!
feedback.empty=No feedback
```

**Arabic (messages_ar.properties):**
```properties
nav.support=الدعم
support.title=مركز المساعدة
support.subtitle=كيف يمكننا مساعدتك؟
support.tab.tickets=تذاكري
support.tab.faq=الأسئلة الشائعة
support.tab.chatbot=المساعد
support.tab.feedback=التقييم

ticket.new=تذكرة جديدة
ticket.subject=الموضوع
ticket.description=الوصف
ticket.category=الفئة
ticket.category.general=عام
ticket.category.technical=تقني
ticket.category.billing=الفوترة
ticket.category.account=الحساب
ticket.category.job=التوظيف
ticket.category.formation=التكوين
ticket.priority=الأولوية
ticket.priority.low=منخفضة
ticket.priority.medium=متوسطة
ticket.priority.high=عالية
ticket.priority.urgent=عاجلة
ticket.status=الحالة
ticket.status.open=مفتوح
ticket.status.in_progress=قيد المعالجة
ticket.status.waiting_reply=في انتظار الرد
ticket.status.resolved=تم الحل
ticket.status.closed=مغلق
ticket.reply=الرد
ticket.close=إغلاق التذكرة
ticket.reopen=إعادة فتح التذكرة
ticket.assign=تعيين
ticket.empty=لا توجد تذاكر
ticket.count={0} تذاكر
ticket.created=تم إنشاء التذكرة بنجاح
ticket.updated=تم تحديث التذكرة
ticket.deleted=تم حذف التذكرة

faq.title=الأسئلة الشائعة
faq.search=البحث في الأسئلة الشائعة...
faq.helpful=مفيد؟
faq.yes=نعم
faq.no=لا
faq.empty=لم يتم العثور على مقالات
faq.new=مقال جديد
faq.edit=تعديل
faq.delete=حذف

chatbot.title=المساعد الافتراضي
chatbot.welcome=مرحبًا! كيف يمكنني مساعدتك؟
chatbot.placeholder=اكتب رسالتك...
chatbot.send=إرسال
chatbot.escalate=تحويل إلى موظف
chatbot.no_match=لست متأكدًا من فهم طلبك. هل تريد إنشاء تذكرة دعم؟

feedback.title=رأيك
feedback.type=النوع
feedback.type.bug=تقرير خطأ
feedback.type.feature=اقتراح
feedback.type.general=عام
feedback.type.complaint=شكوى
feedback.type.praise=إشادة
feedback.rating=التقييم
feedback.comment=التعليق
feedback.submit=إرسال
feedback.submitted=شكرًا لملاحظاتك!
feedback.empty=لا توجد ملاحظات
```

---

## PHASE 8: Seed Data

### Step 8.1 — Insert sample FAQ articles

Add seed data in `DatabaseInitializer` (after table creation, only if table is empty):

```sql
-- Check if empty first: SELECT COUNT(*) FROM faq_articles
INSERT INTO faq_articles (category, question, answer, language) VALUES
('account', 'Comment créer un compte ?', 'Cliquez sur "S''inscrire" sur la page de connexion...', 'fr'),
('account', 'Comment réinitialiser mon mot de passe ?', 'Cliquez sur "Mot de passe oublié" sur la page de connexion...', 'fr'),
('job', 'Comment postuler à une offre ?', 'Allez dans la section "Offres d''emploi" et cliquez sur "Postuler"...', 'fr'),
('formation', 'Les formations sont-elles gratuites ?', 'Certaines formations sont gratuites, d''autres sont payantes...', 'fr'),
('technical', 'Comment contacter le support ?', 'Vous pouvez créer un ticket de support depuis cette page...', 'fr');
```

### Step 8.2 — Insert sample auto-responses

```sql
INSERT INTO auto_responses (trigger_keyword, response_text, category) VALUES
('mot de passe', 'Pour réinitialiser votre mot de passe, allez dans Paramètres > Sécurité ou cliquez sur "Mot de passe oublié" sur la page de connexion.', 'account'),
('inscription', 'Pour vous inscrire, cliquez sur le bouton "S''inscrire" sur la page de connexion et remplissez le formulaire.', 'account'),
('formation', 'Nous proposons des formations gratuites et payantes. Consultez la section Formations pour voir le catalogue complet.', 'formation'),
('emploi', 'Pour trouver un emploi, consultez notre fil d''actualités emploi ou la section Offres d''emploi.', 'job'),
('contact', 'Vous pouvez nous contacter via ce chat ou en créant un ticket de support. Notre équipe vous répondra dans les 24 heures.', 'general');
```

---

## PHASE 9: Verification

### Step 9.1 — Compile
Run `mvn compile -q` and fix all errors.

### Step 9.2 — Run & Test
1. Support nav button appears in sidebar
2. Support view loads with tabs
3. Creating a ticket works
4. Viewing ticket details + replying works
5. FAQ articles display and are searchable
6. Chatbot responds with auto-responses
7. Feedback submission works
8. Admin view shows all tickets (for admin role users)

---

## RULES

1. **NO DAO classes.** Services contain JDBC code directly.
2. **NO JavaFX imports in entity or service classes.**
3. **All UI text via `I18n.get("key")`.**
4. **Singleton pattern** on all services.
5. **Async DB calls** via `javafx.concurrent.Task`.
6. **Follow existing code patterns exactly.**
7. **Table creation in `DatabaseInitializer.java`.**
8. **Use TL* components** for UI.
9. **Handle null safely.**
10. **Log errors** with `logger.error()`, info with `logger.info()`.
11. **Do NOT break existing functionality.** Only add new files and modify DatabaseInitializer + MainView.
