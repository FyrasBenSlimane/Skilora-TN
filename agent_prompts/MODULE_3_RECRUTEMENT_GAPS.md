# MODULE 3: Recrutement International (Gaps) — Agent Prompt

> **Goal:** Complete the missing entities, services, and CRUD operations for the Recrutement International module. Module 3 is PARTIALLY implemented — it has working JobOffer, Application, and JobOpportunity entities + services + controllers. What's MISSING are: Company entity (table exists but no entity/service), Interview entity + service, and HireOffer entity + service. Fill these gaps without breaking existing functionality.

---

## CONTEXT — READ FIRST

### Project Architecture

- **Language:** Java 17, JavaFX 21, Maven
- **Database:** MySQL 8.0 via HikariCP connection pool
- **Pattern:** MVC (Model = entity + service, View = FXML, Controller = JavaFX controller)
- **NO DAO LAYER.** Services directly use `DatabaseConfig.getInstance().getConnection()` with PreparedStatement
- **Singleton pattern** on all services
- **I18n:** All UI strings use `I18n.get("key")` from `com.skilora.utils.I18n`
- **Logging:** SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **UI Components:** Custom TL* components from `com.skilora.framework.components`
- **Async:** All DB calls from controllers use `javafx.concurrent.Task`

### Key File Locations

```
src/main/java/com/skilora/
├── config/DatabaseConfig.java          — DB connection singleton
├── config/DatabaseInitializer.java     — Auto-creates tables on startup
├── model/entity/                       — Pure POJO entities
├── model/service/                      — Service singletons
├── controller/                         — FXML controllers
├── ui/MainView.java                    — Navigation
├── utils/I18n.java                     — i18n

src/main/resources/com/skilora/
├── i18n/messages_*.properties          — 4 bundles
├── view/*.fxml                         — FXML views
```

### What ALREADY EXISTS for Module 3

**Entities:**
- `JobOffer.java` — fields: id, employerId, title, description, location, salaryMin, salaryMax, currency, workType, requiredSkills, status (JobStatus), postedDate, deadline, companyName (transient)
- `Application.java` — fields: id, jobOfferId, candidateProfileId, status (inner enum), appliedDate, coverLetter, customCvUrl, jobTitle/companyName/candidateName/jobLocation (transient)
- `JobOpportunity.java` — for external job feed data

**Services:**
- `JobService.java` — full CRUD for JobOffer + job feed cache
- `ApplicationService.java` — apply, hasApplied, getByProfile, getByJobOffer, updateStatus, count, delete

**Controllers:**
- `ActiveOffersController.java` — admin view of all offers
- `MyOffersController.java` — employer's own offers
- `PostJobController.java` — create/edit job offer form
- `ApplicationsController.java` — job seeker's applications
- `ApplicationInboxController.java` — employer's received applications
- `JobDetailsController.java` — single job offer detail view
- `InterviewsController.java` — **EXISTS but uses ApplicationService to filter INTERVIEW-status applications. Has NO Interview entity/table.**
- `FeedController.java` — external job feed

**Database tables that exist:**
- `companies` — id, owner_id, name, country, industry, website, logo_url, is_verified, size
- `job_offers` — full schema with FK to companies
- `applications` — full schema with FK to job_offers and profiles

**Database tables MISSING:**
- `interviews` — scheduling, video links, timezone handling
- `hire_offers` — formal employment offers after interviews

---

## WHAT'S MISSING — IMPLEMENT THESE

---

## PHASE 1: New Entities

### Step 1.1 — Create Entity: `Company.java`

File: `src/main/java/com/skilora/model/entity/Company.java`

The `companies` table already exists in the DB. Create a matching entity:

```java
package com.skilora.model.entity;

/**
 * Company Entity
 * Represents an employer company on the platform.
 * Maps to the 'companies' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class Company {
    private int id;
    private int ownerId;           // FK to users.id
    private String name;
    private String country;
    private String industry;
    private String website;
    private String logoUrl;
    private boolean verified;
    private String size;           // e.g., "1-10", "11-50", "51-200", "201-500", "500+"

    // Constructors, getters, setters, equals, hashCode, toString
}
```

### Step 1.2 — Create Entity: `Interview.java`

File: `src/main/java/com/skilora/model/entity/Interview.java`

```java
package com.skilora.model.entity;

import java.time.LocalDateTime;

/**
 * Interview Entity
 * Represents a scheduled interview for a job application.
 * Maps to the 'interviews' table.
 */
public class Interview {
    private int id;
    private int applicationId;     // FK to applications.id
    private LocalDateTime scheduledDate;
    private int durationMinutes;   // default 60
    private String type;           // "VIDEO", "IN_PERSON", "PHONE"
    private String location;       // physical address or video link
    private String videoLink;      // Zoom/Jitsi/Teams link
    private String notes;          // interviewer notes
    private String status;         // "SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW"
    private String feedback;       // post-interview feedback
    private int rating;            // 1-5 score
    private String timezone;       // e.g., "Africa/Tunis", "Europe/Paris"
    private LocalDateTime createdDate;

    // Transient fields (from JOINs)
    private String candidateName;
    private String jobTitle;
    private String companyName;

    // Constructors, getters, setters, equals, hashCode, toString
}
```

### Step 1.3 — Create Enum: `InterviewType.java`

File: `src/main/java/com/skilora/model/enums/InterviewType.java`

```java
package com.skilora.model.enums;

public enum InterviewType {
    VIDEO, IN_PERSON, PHONE
}
```

### Step 1.4 — Create Enum: `InterviewStatus.java`

File: `src/main/java/com/skilora/model/enums/InterviewStatus.java`

```java
package com.skilora.model.enums;

public enum InterviewStatus {
    SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
}
```

### Step 1.5 — Create Entity: `HireOffer.java`

File: `src/main/java/com/skilora/model/entity/HireOffer.java`

```java
package com.skilora.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HireOffer Entity
 * Represents a formal employment offer made to a candidate after interview.
 * Maps to the 'hire_offers' table.
 */
public class HireOffer {
    private int id;
    private int applicationId;     // FK to applications.id
    private double salaryOffered;
    private String currency;       // "TND", "EUR", "USD"
    private LocalDate startDate;
    private String contractType;   // "CDI", "CDD", "FREELANCE", "STAGE"
    private String benefits;       // JSON or comma-separated
    private String status;         // "PENDING", "ACCEPTED", "REJECTED", "EXPIRED"
    private LocalDateTime createdDate;
    private LocalDateTime respondedDate;

    // Transient fields
    private String candidateName;
    private String jobTitle;
    private String companyName;

    // Constructors, getters, setters, equals, hashCode, toString
}
```

---

## PHASE 2: Database Schema

### Step 2.1 — Add tables to `DatabaseInitializer.java`

Add `interviews` and `hire_offers` table creation following the existing pattern:

```sql
CREATE TABLE IF NOT EXISTS interviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL,
    scheduled_date DATETIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    type VARCHAR(20) DEFAULT 'VIDEO',
    location VARCHAR(255),
    video_link TEXT,
    notes TEXT,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    feedback TEXT,
    rating INT DEFAULT 0,
    timezone VARCHAR(50) DEFAULT 'Africa/Tunis',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hire_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL,
    salary_offered DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    start_date DATE,
    contract_type VARCHAR(20) DEFAULT 'CDI',
    benefits TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    responded_date DATETIME,
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
);
```

Add indexes:
```sql
CREATE INDEX idx_interviews_application ON interviews(application_id);
CREATE INDEX idx_interviews_status ON interviews(status);
CREATE INDEX idx_interviews_date ON interviews(scheduled_date);
CREATE INDEX idx_hire_offers_application ON hire_offers(application_id);
CREATE INDEX idx_hire_offers_status ON hire_offers(status);
```

---

## PHASE 3: Services

### Step 3.1 — Create `CompanyService.java`

File: `src/main/java/com/skilora/model/service/CompanyService.java`

Singleton service with CRUD:

- `int create(Company c)` — INSERT
- `Company findById(int id)` — SELECT by ID
- `Company findByOwnerId(int ownerId)` — SELECT WHERE owner_id = ?
- `List<Company> findAll()` — SELECT all
- `List<Company> search(String query)` — LIKE on name, country, industry
- `boolean update(Company c)` — UPDATE
- `boolean delete(int id)` — DELETE
- `boolean verify(int id)` — UPDATE is_verified = true
- Private `Company mapResultSet(ResultSet rs)`

### Step 3.2 — Create `InterviewService.java`

File: `src/main/java/com/skilora/model/service/InterviewService.java`

Singleton service with CRUD:

- `int schedule(Interview interview)` — INSERT + update application status to INTERVIEW via `ApplicationService`
- `Interview findById(int id)` — SELECT with JOINs for transient fields
- `List<Interview> findByApplicationId(int applicationId)` — all interviews for an application
- `List<Interview> findByEmployerId(int employerId)` — JOIN applications → job_offers → companies WHERE owner_id = ?
- `List<Interview> findByCandidateId(int profileId)` — JOIN applications WHERE candidate_profile_id = ?
- `List<Interview> findUpcoming(int employerId)` — WHERE scheduled_date > NOW() AND status = 'SCHEDULED'
- `boolean update(Interview interview)` — UPDATE
- `boolean updateStatus(int id, String status)` — UPDATE status only
- `boolean addFeedback(int id, String feedback, int rating)` — UPDATE feedback + rating
- `boolean cancel(int id)` — UPDATE status = 'CANCELLED'
- `boolean delete(int id)` — DELETE
- Private `Interview mapResultSet(ResultSet rs)` — with transient fields from JOINs

### Step 3.3 — Create `HireOfferService.java`

File: `src/main/java/com/skilora/model/service/HireOfferService.java`

Singleton service with CRUD:

- `int create(HireOffer offer)` — INSERT + update application status to OFFER
- `HireOffer findById(int id)` — SELECT with JOINs
- `HireOffer findByApplicationId(int applicationId)` — one offer per application
- `List<HireOffer> findByEmployerId(int employerId)` — JOIN chain
- `List<HireOffer> findByCandidateId(int profileId)` — JOIN chain
- `boolean accept(int id)` — UPDATE status = 'ACCEPTED', respondedDate = NOW(), + update application status to ACCEPTED
- `boolean reject(int id)` — UPDATE status = 'REJECTED', respondedDate = NOW()
- `boolean update(HireOffer offer)` — UPDATE
- `boolean delete(int id)` — DELETE
- Private `HireOffer mapResultSet(ResultSet rs)`

---

## PHASE 4: Update Existing Controllers

### Step 4.1 — Rewrite `InterviewsController.java`

Currently this controller filters applications in INTERVIEW status using `ApplicationService`. Rewrite it to:

1. Use `InterviewService.getInstance()` to load real `Interview` entities
2. Display interview details: scheduled date/time, type, video link, candidate name, job title
3. **CRUD operations:**
   - **Schedule:** "Planifier un entretien" button → dialog with date picker, time picker, type select (Video/In Person/Phone), video link field
   - **Read:** List all interviews with status filters (Scheduled, Completed, Cancelled)
   - **Update:** Edit button to reschedule or update details
   - **Cancel:** Cancel button with confirmation
   - **Feedback:** After interview, add feedback + rating (1-5 stars)
4. Show upcoming interviews prominently
5. All strings via `I18n.get()`

### Step 4.2 — Update `ApplicationInboxController.java`

Add these actions to each application card:
- "Schedule Interview" button → creates an Interview + updates application status
- "Send Offer" button → creates a HireOffer + updates application status
- These buttons should only appear based on application status (e.g., Schedule appears for REVIEWING/PENDING, Send Offer for INTERVIEW)

### Step 4.3 — Update `ApplicationsController.java` (Job Seeker Side)

Add visibility of:
- Interview details (date, time, link) when application status is INTERVIEW
- Hire offer details (salary, contract type, benefits) when status is OFFER
- Accept/Reject offer buttons for the candidate

---

## PHASE 5: I18n Keys

### Step 5.1 — Add keys to all 4 resource bundles

**French (messages.properties / messages_fr.properties):**
```properties
# Company
company.name=Nom de l'entreprise
company.country=Pays
company.industry=Secteur
company.size=Taille
company.verified=Vérifiée
company.website=Site web

# Interviews
interviews.title=Entretiens
interviews.schedule=Planifier un entretien
interviews.reschedule=Reprogrammer
interviews.cancel=Annuler l'entretien
interviews.feedback=Feedback
interviews.rating=Note
interviews.type.video=Vidéo
interviews.type.in_person=En personne
interviews.type.phone=Téléphone
interviews.status.scheduled=Planifié
interviews.status.completed=Terminé
interviews.status.cancelled=Annulé
interviews.status.no_show=Absent
interviews.date=Date et heure
interviews.duration=Durée
interviews.video_link=Lien vidéo
interviews.notes=Notes
interviews.upcoming=Entretiens à venir
interviews.past=Entretiens passés
interviews.empty=Aucun entretien planifié
interviews.confirm.cancel=Êtes-vous sûr de vouloir annuler cet entretien ?
interviews.success.scheduled=Entretien planifié avec succès
interviews.success.cancelled=Entretien annulé
interviews.success.feedback=Feedback ajouté

# Hire Offers
hireoffer.title=Offre d'embauche
hireoffer.salary=Salaire proposé
hireoffer.contract=Type de contrat
hireoffer.contract.cdi=CDI
hireoffer.contract.cdd=CDD
hireoffer.contract.freelance=Freelance
hireoffer.contract.stage=Stage
hireoffer.start_date=Date de début
hireoffer.benefits=Avantages
hireoffer.status.pending=En attente
hireoffer.status.accepted=Acceptée
hireoffer.status.rejected=Refusée
hireoffer.status.expired=Expirée
hireoffer.send=Envoyer une offre
hireoffer.accept=Accepter l'offre
hireoffer.reject=Refuser l'offre
hireoffer.confirm.accept=Êtes-vous sûr de vouloir accepter cette offre ?
hireoffer.confirm.reject=Êtes-vous sûr de vouloir refuser cette offre ?
hireoffer.success.sent=Offre envoyée avec succès
hireoffer.success.accepted=Offre acceptée
hireoffer.success.rejected=Offre refusée
```

**English (messages_en.properties):**
```properties
company.name=Company Name
company.country=Country
company.industry=Industry
company.size=Size
company.verified=Verified
company.website=Website

interviews.title=Interviews
interviews.schedule=Schedule Interview
interviews.reschedule=Reschedule
interviews.cancel=Cancel Interview
interviews.feedback=Feedback
interviews.rating=Rating
interviews.type.video=Video
interviews.type.in_person=In Person
interviews.type.phone=Phone
interviews.status.scheduled=Scheduled
interviews.status.completed=Completed
interviews.status.cancelled=Cancelled
interviews.status.no_show=No Show
interviews.date=Date & Time
interviews.duration=Duration
interviews.video_link=Video Link
interviews.notes=Notes
interviews.upcoming=Upcoming Interviews
interviews.past=Past Interviews
interviews.empty=No interviews scheduled
interviews.confirm.cancel=Are you sure you want to cancel this interview?
interviews.success.scheduled=Interview scheduled successfully
interviews.success.cancelled=Interview cancelled
interviews.success.feedback=Feedback added

hireoffer.title=Hire Offer
hireoffer.salary=Offered Salary
hireoffer.contract=Contract Type
hireoffer.contract.cdi=Permanent
hireoffer.contract.cdd=Fixed-term
hireoffer.contract.freelance=Freelance
hireoffer.contract.stage=Internship
hireoffer.start_date=Start Date
hireoffer.benefits=Benefits
hireoffer.status.pending=Pending
hireoffer.status.accepted=Accepted
hireoffer.status.rejected=Rejected
hireoffer.status.expired=Expired
hireoffer.send=Send Offer
hireoffer.accept=Accept Offer
hireoffer.reject=Reject Offer
hireoffer.confirm.accept=Are you sure you want to accept this offer?
hireoffer.confirm.reject=Are you sure you want to reject this offer?
hireoffer.success.sent=Offer sent successfully
hireoffer.success.accepted=Offer accepted
hireoffer.success.rejected=Offer rejected
```

**Arabic (messages_ar.properties):**
```properties
company.name=اسم الشركة
company.country=البلد
company.industry=القطاع
company.size=الحجم
company.verified=موثّقة
company.website=الموقع الإلكتروني

interviews.title=المقابلات
interviews.schedule=جدولة مقابلة
interviews.reschedule=إعادة جدولة
interviews.cancel=إلغاء المقابلة
interviews.feedback=ملاحظات
interviews.rating=التقييم
interviews.type.video=فيديو
interviews.type.in_person=حضوري
interviews.type.phone=هاتف
interviews.status.scheduled=مجدولة
interviews.status.completed=مكتملة
interviews.status.cancelled=ملغاة
interviews.status.no_show=غياب
interviews.date=التاريخ والوقت
interviews.duration=المدة
interviews.video_link=رابط الفيديو
interviews.notes=ملاحظات
interviews.upcoming=المقابلات القادمة
interviews.past=المقابلات السابقة
interviews.empty=لا توجد مقابلات مجدولة
interviews.confirm.cancel=هل أنت متأكد من إلغاء هذه المقابلة؟
interviews.success.scheduled=تم جدولة المقابلة بنجاح
interviews.success.cancelled=تم إلغاء المقابلة
interviews.success.feedback=تمت إضافة الملاحظات

hireoffer.title=عرض التوظيف
hireoffer.salary=الراتب المقترح
hireoffer.contract=نوع العقد
hireoffer.contract.cdi=عقد دائم
hireoffer.contract.cdd=عقد محدد المدة
hireoffer.contract.freelance=عمل حر
hireoffer.contract.stage=تربّص
hireoffer.start_date=تاريخ البدء
hireoffer.benefits=المزايا
hireoffer.status.pending=في الانتظار
hireoffer.status.accepted=مقبول
hireoffer.status.rejected=مرفوض
hireoffer.status.expired=منتهي الصلاحية
hireoffer.send=إرسال عرض
hireoffer.accept=قبول العرض
hireoffer.reject=رفض العرض
hireoffer.confirm.accept=هل أنت متأكد من قبول هذا العرض؟
hireoffer.confirm.reject=هل أنت متأكد من رفض هذا العرض؟
hireoffer.success.sent=تم إرسال العرض بنجاح
hireoffer.success.accepted=تم قبول العرض
hireoffer.success.rejected=تم رفض العرض
```

---

## PHASE 6: Verification

### Step 6.1 — Compile
Run `mvn compile -q` and fix all errors.

### Step 6.2 — Run & Test
1. InterviewsView loads and shows real interview data
2. Scheduling an interview from ApplicationInbox works
3. Sending a hire offer works
4. Candidate can see interview details and accept/reject offers
5. All CRUD operations work end-to-end

---

## RULES

1. **NO DAO classes.** Services contain JDBC code directly.
2. **NO JavaFX imports in entity or service classes.**
3. **All UI text via `I18n.get("key")`.**
4. **Singleton pattern** on all services.
5. **Async DB calls** via `javafx.concurrent.Task`.
6. **Do NOT break existing code.** The existing controllers and services for JobOffer and Application must continue to work.
7. **Follow existing patterns exactly** — same code style, same error handling, same naming.
8. **Table creation in `DatabaseInitializer.java`** — add new tables in the `initialize()` method.
9. **Use TL* components** for UI.
10. **Handle null safely.**
