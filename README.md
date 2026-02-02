# Skilora Tunisia 🌍

**Where Talent Meets Opportunity | Connecting Tunisia's Youth to the World**

> *"Empower Talent. Eliminate Barriers."*

---

## 🚀 The Vision

**Skilora Tunisia** isn't just a platform; it's a bridge to the future.

In a landscape where 41% of Tunisia's youth face unemployment, Skilora emerges as a vital ecosystem designed to unlock potential. We are transforming the journey from education to employment by connecting skilled Tunisian job seekers directly with global career opportunities.

By integrating intelligent skills matching, biometric verification, and seamless profile management, Skilora provides a trustworthy and efficient marketplace for candidates to showcase their true value and for employers to find the perfect fit—instantly.

---

## 🌟 Why Skilora?

*   **🌍 Global Reach, Local Impact**: Opening doors to international markets while nurturing local roots.
*   **🧠 Intelligent Matching**: No more aimless applying. Our matching algorithms ensure candidates see roles they are truly qualified for.
*   **🛡️ Trust & Security**: With state-of-the-art **Biometric Face Recognition**, we ensure that every profile is authentic, building trust between employers and candidates.
*   **💻 Seamless Ecosystem**: A dual-client architecture featuring a robust JavaFX Desktop Application and a planned Symfony Web Portal.

---

## 📋 Project Context

*Built with passion by students at **ESPRIT - Honoris United Universities**.*

This application serves as our **Projet Intégré de Développement (PIDEV)**, showcasing a modern, scalable architecture that solves real-world problems.

**Module:** 3A - Web Java | **Duration:** Sprint 0-1 (Jan-Mar 2026) | **Team:** 6 Developers

---

## 🎯 Key Features

- **Dual-Client Architecture**: JavaFX Desktop + Symfony Web Application
- **Intelligent Job Matching**: Skill-based matching algorithm for opportunities
- **Biometric Integration**: Face recognition for profile authentication
- **Profile Management**: Comprehensive talent profiles with experience & skills
- **Job Feed System**: Dynamic job aggregation and recommendations
- **Modular UI Framework**: Reusable, team-friendly JavaFX components
- **Database Sync**: Shared MySQL database between clients

---

## 🏗️ Project Structure

```
skilora-tunisia/
├── src/
│   ├── main/java/com/skilora/
│   │   ├── Main.java                    # Application entry point
│   │   ├── config/                      # Database & app configuration
│   │   ├── dao/                         # Data Access Objects
│   │   ├── models/                      # Domain models (User, Job, etc.)
│   │   ├── services/                    # Business logic
│   │   ├── integration/                 # Auth, Navigation, External APIs
│   │   ├── framework/                   # Reusable UI components
│   │   │   ├── components/              # TLButton, TLCard, etc.
│   │   │   ├── layouts/                 # TLAppLayout patterns
│   │   │   └── themes/                  # Centralized theming
│   │   └── ui/views/                    # Application views
│   └── resources/                       # Styles, fonts, images
├── python/                              # Python services
│   ├── face_recognition_service.py
│   ├── job_feed_crawler.py
│   └── job_feed_config.json
├── data/                                # Sample data & configs
├── biometric_data/                      # Face encoding storage
├── pom.xml                              # Maven configuration
└── skilora.sql                          # Database schema

```

---

## 🛠️ Tech Stack

### Backend/Desktop
- **Language**: Java 17
- **Framework**: JavaFX 21 (Desktop UI)
- **Build Tool**: Maven
- **Database**: MySQL 8.0+
- **ORM**: Custom DAO pattern

### Services
- **Python**: Biometric recognition & Job crawling
- **Face Recognition**: OpenCV/dlib integration
- **HTTP Client**: Java's HttpClient for external APIs

### Frontend Components
- **Modular UI Framework**: Custom JavaFX components
- **Theme System**: Centralized styling (Material Design-inspired)
- **Responsive Layouts**: Adaptive components

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+
- Python 3.9+ (for biometric & job feed services)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/skilora-tunisia.git
   cd skilora-tunisia
   ```

2. **Setup Database**
   ```bash
   mysql -u root -p < skilora.sql
   ```
   Update `src/main/resources/database.properties` with your credentials.

3. **Build with Maven**
   ```bash
   mvn clean install
   ```

4. **Install Python Dependencies** (Optional - for biometric features)
   ```bash
   pip install -r requirements.txt
   cd python && pip install -r requirements_jobs.txt
   ```

5. **Run the Application**
   ```bash
   mvn javafx:run
   # Or run Main.java directly from your IDE
   ```

---

## 📦 Core Modules

### Data Access Layer (DAO)
- `UserDAO` - User profile management
- `JobOfferDAO` - Job listing operations
- `ProfileDAO` - Talent profile handling
- `SkillDAO` - Skill inventory
- `ExperienceDAO` - Work experience tracking

### Business Services
- Authentication & Authorization
- Profile Matching Engine
- Job Recommendation Algorithm
- Notification System

### UI Components
- **TLButton**: Customizable buttons (Primary, Outline, Success variants)
- **TLCard**: Reusable card containers
- **TLAppLayout**: Standard application layout pattern
- **Custom Theming**: Material Design color palette

---

## 🎨 Framework Usage

### Quick Start with Components

```java
import com.skilora.framework.components.*;
import com.skilora.framework.themes.Theme;

// Create a primary button
TLButton submitBtn = new TLButton("Submit", TLButton.ButtonVariant.PRIMARY);

// Create a card
TLCard profileCard = new TLCard();
profileCard.setHeader("User Profile");

// Use theme colors
String accentColor = Theme.PRIMARY; // #1E40AF
```

### Theme Customization
All theming is centralized in `Theme.java` for consistent design across the application.

---

## 🔄 Sprints Overview

| Sprint | Duration | Focus | Status |
|--------|----------|-------|--------|
| **Sprint 0** | Weeks 1-2 | Project planning, design, DB setup | ✅ Complete |
| **Sprint 1 (Java)** | Weeks 3-7 | JavaFX desktop app development | 🚀 In Progress |
| **Sprint 2 (Web)** | Future | Symfony web client + API | ⏳ Planned |

---

## 📊 Database Schema

Key entities:
- **Users**: Profiles with authentication
- **Jobs**: Job opportunities with requirements
- **Skills**: Skill inventory with proficiency levels
- **Experience**: Work history and background
- **Matches**: Job-to-talent matching records
- **Preferences**: User job preferences & filters

See [skilora.sql](skilora.sql) for complete schema.

---

## 🔐 Security Features

- Biometric authentication (face recognition)
- Encrypted credential storage
- Role-based access control (RBAC)
- Secure database transactions
- Input validation & SQL injection prevention

---

## 🤝 Contributing

This is an educational PIDEV project. For contributions:
1. Follow the team's coding standards
2. Update documentation when adding features
3. Test thoroughly before submitting
4. Use meaningful commit messages

---

## 📝 Documentation

- [Framework README](FRAMEWORK_README.md) - UI Component Documentation
- [Implementation Progress](IMPLEMENTATION_PROGRESS.md) - Current development status
- [Skilora File Map](SKILORA_FILEMAP.md) - Detailed file structure
- [Component Showcase](COMPONENT_SHOWCASE.md) - UI Component Examples

---

## 📧 Contact

**Project Lead**: Team Skilora Tunisia  
**Institution**: ESPRIT - Honoris United Universities  
**Email**: skilora.tunisia@esprit.tn  
**Academic Year**: 2025-2026

---

## 📄 License

This project is developed for educational purposes at ESPRIT.

---

**Skilora - Where Skills Flourish** 🌟
