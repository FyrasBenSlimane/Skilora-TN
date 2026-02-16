# MODULE 2: Formation & Certification — Agent Prompt

> **Goal:** Implement the complete Formation & Certification module for the Skilora Tunisia JavaFX application. This module manages training courses, enrollments, quizzes, and certificate generation. Follow the existing MVC architecture exactly — NO DAO layer, services handle both business logic AND data access via JDBC.

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
- **UI Components:** Custom TL* components from `com.skilora.framework.components` (TLButton, TLCard, TLBadge, TLTextField, TLSelect, TLAlert, TLDialog, etc.)
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

### Existing Entity Pattern (follow exactly)

```java
package com.skilora.model.entity;

/**
 * EntityName Entity
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class EntityName {
    private int id;
    private String fieldName;
    // ... fields

    public EntityName() {}
    public EntityName(/* key fields */) { /* set them */ }

    // Getters and setters for all fields
    // equals(), hashCode() on id
    // toString()
}
```

### Existing Service Pattern (follow exactly)

```java
package com.skilora.model.service;

import com.skilora.config.DatabaseConfig;
import java.sql.*;
import java.util.*;

public class XxxService {
    private static final Logger logger = LoggerFactory.getLogger(XxxService.class);
    private static volatile XxxService instance;

    private XxxService() {}

    public static XxxService getInstance() {
        if (instance == null) {
            synchronized (XxxService.class) {
                if (instance == null) { instance = new XxxService(); }
            }
        }
        return instance;
    }

    public int create(Entity entity) throws SQLException {
        String sql = "INSERT INTO table_name (...) VALUES (?, ?, ...)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // set params
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // findById, findAll, update, delete follow same pattern
    // Private mapResultSet(ResultSet rs) method to convert rows to entities
}
```

### Existing Controller Pattern (follow exactly)

```java
package com.skilora.controller;

import com.skilora.utils.I18n;
// ... imports

public class XxxController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(XxxController.class);

    @FXML private Label statsLabel;
    @FXML private VBox container;
    // ... FXML fields

    private final XxxService xxxService = XxxService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyI18n();
        setupFilters();
        loadData();
    }

    private void loadData() {
        Task<List<Entity>> task = new Task<>() {
            @Override protected List<Entity> call() throws Exception {
                return xxxService.findAll();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> renderData(task.getValue())));
        task.setOnFailed(e -> logger.error("Load failed", task.getException()));
        new Thread(task).start();
    }
    // ...
}
```

---

## WHAT EXISTS NOW

The `FormationsController.java` exists but uses **hardcoded sample data** with an inner static class `Formation`. There is **no database backing**, no entities, no service, no CRUD. The `FormationsView.fxml` exists as a basic shell. This controller must be **completely rewritten** to use real entities and database-backed CRUD.

---

## PHASE 1: Database Schema & Entities

### Step 1.1 — Add table creation to `DatabaseInitializer.java`

Add the following tables to `DatabaseInitializer.initialize()`. Follow the same `if (!tableExists(...))` + `createXxxTable(stmt)` pattern already used for `applications`, `password_reset_tokens`, etc.

**Tables to create:**

```sql
CREATE TABLE IF NOT EXISTS formations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    duration_hours INT DEFAULT 0,
    cost DECIMAL(10,2) DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'TND',
    provider VARCHAR(100),
    image_url TEXT,
    level VARCHAR(30) DEFAULT 'BEGINNER',
    is_free BOOLEAN DEFAULT TRUE,
    created_by INT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS formation_modules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    formation_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    content_url TEXT,
    duration_minutes INT DEFAULT 0,
    order_index INT DEFAULT 0,
    FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    formation_id INT NOT NULL,
    user_id INT NOT NULL,
    status VARCHAR(30) DEFAULT 'IN_PROGRESS',
    progress DECIMAL(5,2) DEFAULT 0.00,
    enrolled_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_date DATETIME,
    UNIQUE KEY uq_enrollment (formation_id, user_id),
    FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quizzes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    formation_id INT NOT NULL,
    module_id INT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    pass_score INT DEFAULT 70,
    max_attempts INT DEFAULT 3,
    time_limit_minutes INT DEFAULT 30,
    FOREIGN KEY (formation_id) REFERENCES formations(id) ON DELETE CASCADE,
    FOREIGN KEY (module_id) REFERENCES formation_modules(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(500),
    option_b VARCHAR(500),
    option_c VARCHAR(500),
    option_d VARCHAR(500),
    correct_option CHAR(1) NOT NULL,
    points INT DEFAULT 1,
    order_index INT DEFAULT 0,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    user_id INT NOT NULL,
    score INT DEFAULT 0,
    max_score INT DEFAULT 0,
    passed BOOLEAN DEFAULT FALSE,
    attempt_number INT DEFAULT 1,
    taken_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    time_spent_seconds INT DEFAULT 0,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS certificates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL UNIQUE,
    certificate_number VARCHAR(50) NOT NULL UNIQUE,
    issued_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    qr_code TEXT,
    hash_value VARCHAR(128),
    pdf_url TEXT,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE
);
```

Also add indexes:
```sql
CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_enrollments_formation ON enrollments(formation_id);
CREATE INDEX idx_quiz_results_user ON quiz_results(user_id);
CREATE INDEX idx_quiz_results_quiz ON quiz_results(quiz_id);
CREATE INDEX idx_formations_category ON formations(category);
CREATE INDEX idx_formations_status ON formations(status);
```

### Step 1.2 — Create Enum: `FormationLevel.java`

File: `src/main/java/com/skilora/model/enums/FormationLevel.java`

```java
package com.skilora.model.enums;

public enum FormationLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}
```

### Step 1.3 — Create Enum: `EnrollmentStatus.java`

File: `src/main/java/com/skilora/model/enums/EnrollmentStatus.java`

```java
package com.skilora.model.enums;

public enum EnrollmentStatus {
    IN_PROGRESS, COMPLETED, ABANDONED
}
```

### Step 1.4 — Create Entity: `Formation.java`

File: `src/main/java/com/skilora/model/entity/Formation.java`

Fields: `id`, `title`, `description`, `category`, `durationHours`, `cost`, `currency`, `provider`, `imageUrl`, `level` (FormationLevel), `isFree`, `createdBy`, `createdDate` (LocalDateTime), `status`.

Follow the exact entity pattern above. Pure POJO, no JavaFX imports.

### Step 1.5 — Create Entity: `FormationModule.java`

File: `src/main/java/com/skilora/model/entity/FormationModule.java`

Fields: `id`, `formationId`, `title`, `description`, `contentUrl`, `durationMinutes`, `orderIndex`.

### Step 1.6 — Create Entity: `Enrollment.java`

File: `src/main/java/com/skilora/model/entity/Enrollment.java`

Fields: `id`, `formationId`, `userId`, `status` (EnrollmentStatus), `progress` (double), `enrolledDate` (LocalDateTime), `completedDate` (LocalDateTime).

Transient fields (from JOINs): `formationTitle`, `userName`.

### Step 1.7 — Create Entity: `Quiz.java`

File: `src/main/java/com/skilora/model/entity/Quiz.java`

Fields: `id`, `formationId`, `moduleId`, `title`, `description`, `passScore`, `maxAttempts`, `timeLimitMinutes`.

### Step 1.8 — Create Entity: `QuizQuestion.java`

File: `src/main/java/com/skilora/model/entity/QuizQuestion.java`

Fields: `id`, `quizId`, `questionText`, `optionA`, `optionB`, `optionC`, `optionD`, `correctOption` (char), `points`, `orderIndex`.

### Step 1.9 — Create Entity: `QuizResult.java`

File: `src/main/java/com/skilora/model/entity/QuizResult.java`

Fields: `id`, `quizId`, `userId`, `score`, `maxScore`, `passed`, `attemptNumber`, `takenDate` (LocalDateTime), `timeSpentSeconds`.

### Step 1.10 — Create Entity: `Certificate.java`

File: `src/main/java/com/skilora/model/entity/Certificate.java`

Fields: `id`, `enrollmentId`, `certificateNumber`, `issuedDate` (LocalDateTime), `qrCode`, `hashValue`, `pdfUrl`.

---

## PHASE 2: Services (CRUD + Business Logic)

### Step 2.1 — Create `FormationService.java`

File: `src/main/java/com/skilora/model/service/FormationService.java`

Singleton service with full CRUD:

- `int createFormation(Formation f)` — INSERT, return generated ID
- `Formation findById(int id)` — SELECT by ID
- `List<Formation> findAll()` — SELECT all, ORDER BY created_date DESC
- `List<Formation> findByCategory(String category)` — filtered query
- `List<Formation> findByStatus(String status)` — filtered query
- `List<Formation> search(String query)` — LIKE search on title, description, provider
- `boolean update(Formation f)` — UPDATE by ID
- `boolean delete(int id)` — DELETE by ID
- `long countAll()` — SELECT COUNT(*)
- `long countByCategory(String category)` — count filtered
- Private `Formation mapResultSet(ResultSet rs)` method

### Step 2.2 — Create `FormationModuleService.java`

File: `src/main/java/com/skilora/model/service/FormationModuleService.java`

CRUD:
- `int create(FormationModule m)`
- `List<FormationModule> findByFormationId(int formationId)` — ORDER BY order_index
- `boolean update(FormationModule m)`
- `boolean delete(int id)`

### Step 2.3 — Create `EnrollmentService.java`

File: `src/main/java/com/skilora/model/service/EnrollmentService.java`

CRUD + business logic:
- `int enroll(int formationId, int userId)` — INSERT (check UNIQUE constraint, return -1 if already enrolled)
- `boolean isEnrolled(int formationId, int userId)` — SELECT 1 check
- `Enrollment findById(int id)`
- `List<Enrollment> findByUserId(int userId)` — JOIN formations for title
- `List<Enrollment> findByFormationId(int formationId)` — JOIN users for name
- `boolean updateProgress(int enrollmentId, double progress)` — UPDATE progress, if progress >= 100 set status=COMPLETED and completedDate
- `boolean updateStatus(int enrollmentId, EnrollmentStatus status)`
- `boolean unenroll(int enrollmentId)` — DELETE
- `long countByFormation(int formationId)`

### Step 2.4 — Create `QuizService.java`

File: `src/main/java/com/skilora/model/service/QuizService.java`

CRUD:
- `int createQuiz(Quiz quiz)`
- `Quiz findById(int id)`
- `List<Quiz> findByFormationId(int formationId)`
- `boolean update(Quiz quiz)`
- `boolean delete(int id)`

Questions CRUD:
- `int addQuestion(QuizQuestion q)`
- `List<QuizQuestion> getQuestions(int quizId)` — ORDER BY order_index
- `boolean updateQuestion(QuizQuestion q)`
- `boolean deleteQuestion(int id)`

Results:
- `int submitResult(QuizResult result)`
- `List<QuizResult> getResultsByUser(int userId)`
- `List<QuizResult> getResultsByQuiz(int quizId)`
- `QuizResult getBestResult(int quizId, int userId)` — ORDER BY score DESC LIMIT 1
- `int getAttemptCount(int quizId, int userId)` — SELECT COUNT(*)

### Step 2.5 — Create `CertificateService.java`

File: `src/main/java/com/skilora/model/service/CertificateService.java`

CRUD + generation:
- `int generate(int enrollmentId)` — create certificate with auto-generated certificateNumber (e.g., `"CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`), hashValue from SHA-256 of certificateNumber+enrollmentId
- `Certificate findByEnrollmentId(int enrollmentId)`
- `Certificate findByCertificateNumber(String certNumber)` — for verification
- `List<Certificate> findByUserId(int userId)` — JOIN enrollments
- `boolean delete(int id)`

---

## PHASE 3: Rewrite Controller & View

### Step 3.1 — Rewrite `FormationsController.java`

**Delete the inner static `Formation` class and `getSampleFormations()` method entirely.**

Rewrite the controller to:
1. Use `FormationService.getInstance()` and `EnrollmentService.getInstance()`
2. Load formations from DB with `Task` (async)
3. Support full CRUD:
   - **Create:** "Nouvelle Formation" button opens a dialog/form to add a formation
   - **Read:** Display formations in `FlowPane` as TLCards (keep existing card UI pattern)
   - **Update:** Edit button on each card (for admins/trainers)
   - **Delete:** Delete button with confirmation dialog
   - **Enroll:** Enroll button for job seekers (calls `EnrollmentService.enroll()`)
4. Keep the existing filter categories and search functionality
5. Show enrollment progress bar if user is enrolled
6. All strings via `I18n.get()`
7. `setCurrentUser(User user)` method so MainView can pass the logged-in user

### Step 3.2 — Update `FormationsView.fxml`

Update FXML to add a "Nouvelle Formation" button in the header (visible to ADMIN/FORMATEUR roles). Keep the rest of the layout similar.

### Step 3.3 — Create `QuizView.fxml` + `QuizController.java` (Optional Dialog)

A dialog/overlay that shows quiz questions one by one:
- Shows question text + 4 radio options
- Next/Previous navigation
- Timer display
- Submit button → calls `QuizService.submitResult()`
- Shows pass/fail result

This can be a simple TLDialog or a separate FXML loaded as a dialog.

---

## PHASE 4: Navigation Integration

### Step 4.1 — Update `MainView.java`

The `showFormationsView()` method already exists. Update it so the controller gets `currentUser` via `setCurrentUser()`:

```java
private void showFormationsView() {
    centerStack.getChildren().clear();
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/FormationsView.fxml"));
        VBox content = loader.load();
        FormationsController controller = loader.getController();
        if (controller != null) {
            controller.setCurrentUser(currentUser);
        }
        // ... scroll area, cache, animate
    } catch (Exception e) {
        logger.error("Failed to load FormationsView", e);
    }
}
```

---

## PHASE 5: I18n Keys

### Step 5.1 — Add keys to all 4 resource bundles

Add these keys to `messages.properties`, `messages_fr.properties`, `messages_en.properties`, `messages_ar.properties`:

**French (messages.properties and messages_fr.properties):**
```properties
# Formations Module
formations.title=Formations
formations.subtitle=Développez vos compétences
formations.new=Nouvelle Formation
formations.edit=Modifier
formations.delete=Supprimer
formations.enroll=S'inscrire
formations.unenroll=Se désinscrire
formations.enrolled=Inscrit
formations.count={0} formations disponibles
formations.not_found=Aucune formation trouvée
formations.search=Rechercher une formation...
formations.filter.all=Toutes
formations.filter.development=Développement
formations.filter.design=Design
formations.filter.marketing=Marketing
formations.filter.data_science=Data Science
formations.filter.languages=Langues
formations.filter.soft_skills=Soft Skills
formations.level.beginner=Débutant
formations.level.intermediate=Intermédiaire
formations.level.advanced=Avancé
formations.duration={0} heures
formations.free=Gratuit
formations.price=Prix: {0}
formations.provider=Fournisseur
formations.completion={0}% complété
formations.enroll.free=S'inscrire (Gratuit)
formations.enroll.paid=S'inscrire ({0})
formations.confirm.delete=Êtes-vous sûr de vouloir supprimer cette formation ?
formations.success.created=Formation créée avec succès
formations.success.updated=Formation mise à jour
formations.success.deleted=Formation supprimée
formations.success.enrolled=Inscription réussie

# Quiz
quiz.title=Quiz
quiz.question=Question {0} sur {1}
quiz.submit=Soumettre
quiz.next=Suivant
quiz.previous=Précédent
quiz.timer=Temps restant: {0}
quiz.result.pass=Félicitations ! Vous avez réussi !
quiz.result.fail=Vous n'avez pas atteint le score minimum.
quiz.score=Score: {0}/{1}

# Certificate
certificate.title=Certificat
certificate.number=N° Certificat
certificate.issued=Délivré le
certificate.download=Télécharger PDF
certificate.verify=Vérifier
```

**English (messages_en.properties):**
```properties
formations.title=Courses
formations.subtitle=Develop your skills
formations.new=New Course
formations.edit=Edit
formations.delete=Delete
formations.enroll=Enroll
formations.unenroll=Unenroll
formations.enrolled=Enrolled
formations.count={0} courses available
formations.not_found=No courses found
formations.search=Search for a course...
formations.level.beginner=Beginner
formations.level.intermediate=Intermediate
formations.level.advanced=Advanced
formations.duration={0} hours
formations.free=Free
formations.price=Price: {0}
formations.provider=Provider
formations.completion={0}% completed
formations.enroll.free=Enroll (Free)
formations.enroll.paid=Enroll ({0})
formations.confirm.delete=Are you sure you want to delete this course?
formations.success.created=Course created successfully
formations.success.updated=Course updated
formations.success.deleted=Course deleted
formations.success.enrolled=Successfully enrolled

quiz.title=Quiz
quiz.question=Question {0} of {1}
quiz.submit=Submit
quiz.next=Next
quiz.previous=Previous
quiz.timer=Time remaining: {0}
quiz.result.pass=Congratulations! You passed!
quiz.result.fail=You did not reach the minimum score.
quiz.score=Score: {0}/{1}

certificate.title=Certificate
certificate.number=Certificate No.
certificate.issued=Issued on
certificate.download=Download PDF
certificate.verify=Verify
```

**Arabic (messages_ar.properties):**
```properties
formations.title=التكوينات
formations.subtitle=طوّر مهاراتك
formations.new=تكوين جديد
formations.edit=تعديل
formations.delete=حذف
formations.enroll=التسجيل
formations.unenroll=إلغاء التسجيل
formations.enrolled=مسجّل
formations.count={0} تكوينات متاحة
formations.not_found=لم يتم العثور على تكوينات
formations.search=البحث عن تكوين...
formations.level.beginner=مبتدئ
formations.level.intermediate=متوسط
formations.level.advanced=متقدم
formations.duration={0} ساعات
formations.free=مجاني
formations.price=السعر: {0}
formations.provider=المزوّد
formations.completion={0}% مكتمل
formations.enroll.free=تسجيل (مجاني)
formations.enroll.paid=تسجيل ({0})
formations.confirm.delete=هل أنت متأكد من حذف هذا التكوين؟
formations.success.created=تم إنشاء التكوين بنجاح
formations.success.updated=تم تحديث التكوين
formations.success.deleted=تم حذف التكوين
formations.success.enrolled=تم التسجيل بنجاح

quiz.title=اختبار
quiz.question=السؤال {0} من {1}
quiz.submit=إرسال
quiz.next=التالي
quiz.previous=السابق
quiz.timer=الوقت المتبقي: {0}
quiz.result.pass=تهانينا! لقد نجحت!
quiz.result.fail=لم تحقق الدرجة الدنيا.
quiz.score=النتيجة: {0}/{1}

certificate.title=شهادة
certificate.number=رقم الشهادة
certificate.issued=صدرت في
certificate.download=تحميل PDF
certificate.verify=تحقق
```

---

## PHASE 6: Verification

### Step 6.1 — Compile
Run `mvn compile -q` and fix any errors.

### Step 6.2 — Run
Run the application and test:
1. Formations view loads without errors
2. Filter and search work
3. Creating a new formation saves to DB
4. Editing and deleting work
5. Enrollment works for job seekers
6. Quiz flow works if implemented

---

## RULES

1. **NO DAO classes.** Services contain JDBC code directly.
2. **NO JavaFX imports in entity or service classes.**
3. **All UI text via `I18n.get("key")`** — never hardcode display strings.
4. **Singleton pattern** on all services.
5. **Async DB calls** from controllers using `javafx.concurrent.Task`.
6. **Follow existing code style exactly** — same import order, same comment style, same error handling.
7. **Table creation in `DatabaseInitializer.java`** — not in separate SQL files.
8. **Use TL* components** (TLButton, TLCard, TLBadge, TLTextField, TLSelect) not raw JavaFX controls where a TL equivalent exists.
9. **Handle null safely** — all getter usage must account for null.
10. **Log errors** with `logger.error()`, info with `logger.info()`.
