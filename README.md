<div align="center">

<img src="src/main/resources/com/skilora/assets/logo.png" alt="Skilora Logo" width="120" height="120" onerror="this.style.display='none'"/>

# 🌟 Skilora TN

### *Tunisia's All-in-One Talent & Career Ecosystem*

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0078D4?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Academic-green?style=for-the-badge)](LICENSE)

---

**Connecting Tunisia's Youth to Global Opportunities.**  
*Secure · Intelligent · Limitless*

</div>

---

## 👋 Welcome to Skilora

**Skilora** is a modern, full-featured desktop platform built for the Tunisian job market — designed to bridge the gap between talented professionals and forward-thinking employers. Whether you're a fresh graduate looking for your first opportunity, an experienced developer seeking the next challenge, or a company ready to build your dream team, Skilora gives you everything you need in one place.

> Built with love in Tunisia 🇹🇳 by ESPRIT engineering students.

---

## 🎯 What Is Skilora?

Skilora is a **talent recruitment and career development ecosystem** that covers the entire professional lifecycle:

- **Find work** — Browse curated job listings tailored to your skills
- **Get hired** — Go through a structured interview → offer → contract pipeline
- **Grow your skills** — Enroll in formations, take quizzes, earn certificates
- **Connect with professionals** — Build your network, join groups, attend events
- **Manage your career finances** — View contracts, payslips, and salary history

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| Apache Maven | 3.9+ |
| MySQL | 8.0+ |
| Python | 3.9+ *(for AI face recognition service)* |

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/FyrasBenSlimane/Skilora-TN.git
cd Skilora-TN

# 2. Set up the database
mysql -u root -p < skilora.sql

# 3. Configure your DB credentials
# Edit src/main/resources/config/database.properties

# 4. (Optional) Start the AI face recognition service
cd python/recruitment_api
pip install -r requirements.txt
python main.py

# 5. Build & Run
mvn clean javafx:run
```

---

## ✨ Features at a Glance

### 🏠 For Talent / Candidates

| Feature | Description |
|--------|-------------|
| 🔐 **Biometric Login** | Face ID authentication powered by Python AI |
| 📄 **Smart Profile** | Skills, experience, certifications, portfolio |
| 🔍 **Job Feed** | AI-filtered listings with match scoring |
| 📬 **One-Click Apply** | CV upload + cover letter in seconds |
| 🎓 **Formations** | Enroll in courses, complete quizzes, earn certificates |
| 💬 **Messaging** | Real-time chat with voice messages |
| 🤝 **Mentorship** | Connect with experienced mentors |
| 💰 **Finance Dashboard** | View contracts, payslips, salary history |

### 🏢 For Employers / Companies

| Feature | Description |
|--------|-------------|
| 📋 **Job Wizard** | Step-by-step offer creation with AI suggestions |
| 📥 **Application Inbox** | Kanban-style candidate pipeline |
| 📅 **Interview Scheduler** | Built-in calendar with countdown timers |
| 📜 **Hire Offers** | Formal offer generation (CDI, CDD, Freelance, Internship) |
| ✍️ **Digital Contracts** | E-signature with PDF generation |
| 💳 **Payroll Management** | Generate payslips, track CNSS & IRPP |
| 📊 **Employer Dashboard** | Real-time analytics and insights |

### 🛡️ For Admins

| Feature | Description |
|--------|-------------|
| 👥 **User Management** | Full CRUD with role control |
| 🎓 **Formation Admin** | Create courses, manage modules, set quizzes |
| 🎫 **Support Center** | SLA-tracked tickets with AI reply suggestions |
| 🚩 **Reports & Moderation** | Community health management |
| 💹 **Finance Admin** | Escrow, tax brackets, exchange rates |
| 📣 **Notifications** | Platform-wide broadcast system |

---

## 🌍 Multilingual Support

Skilora supports **3 languages** natively with full RTL support for Arabic:

| Language | Code | Status |
|----------|------|--------|
| 🇫🇷 Français | `fr` | ✅ Default |
| 🇬🇧 English | `en` | ✅ Full |
| 🇸🇦 العربية | `ar` | ✅ Full RTL |

---

## 🤖 AI-Powered Features

- **Face Recognition Login** — Python-based biometric auth
- **Formation Chatbot** — Gemini AI assistant for training guidance
- **Support AI** — Intelligent ticket reply suggestions
- **Job Matching** — Smart relevance scoring for candidates
- **Training Recommendations** — Suggest formations based on rejected applications

---

## 🏗️ Architecture

```
Skilora TN
├── src/main/java/com/skilora/
│   ├── config/          # DB init, seeding, configuration
│   ├── framework/       # Custom UI components (TLButton, TLCard, TLBadge...)
│   ├── community/       # Posts, connections, messaging, events, groups, blog
│   ├── recruitment/     # Jobs, applications, interviews, hire offers
│   ├── formation/       # Courses, modules, quizzes, certificates
│   ├── finance/         # Contracts, payroll, bank accounts, escrow
│   ├── support/         # Tickets, FAQ, chatbot, feedback
│   ├── user/            # Profiles, settings, auth, biometrics
│   └── ui/              # Main window, navigation, theme
├── src/main/resources/
│   ├── i18n/            # FR / EN / AR translations
│   ├── view/            # FXML layouts per module
│   └── ui/styles/       # CSS theme system
└── python/
    ├── face_recognition_service.py
    └── recruitment_api/  # FastAPI AI endpoints
```

---

## 🎨 Tech Stack

```
Frontend   →  JavaFX 21 + Custom Design System (TL Components)
Backend    →  Java 17 + JDBC (no ORM)
Database   →  MySQL 8.0
AI/ML      →  Python 3 + face_recognition + Google Gemini API
PDF        →  iText / custom certificate renderer
Testing    →  JUnit 5 + JaCoCo coverage
Build      →  Apache Maven
```

---

## 📸 Module Overview

```
🔐 Login / Register     →  Biometric + OTP password reset
🏠 Dashboard            →  Role-aware home with stats + activity
💼 Job Feed             →  Filter · Sort · Save · Apply
📥 Employer Inbox       →  Review → Interview → Offer → Hire
📅 Interview Calendar   →  Schedule · Complete · Feedback
🎓 Formations           →  Enroll · Learn · Quiz · Certificate
💬 Community            →  Feed · Connections · Groups · Events · Blog
🎫 Support              →  Tickets · FAQ · AI Chatbot · Feedback
💰 Finance              →  Contracts · Payslips · Bank · Escrow
⚙️ Settings             →  Profile · Security · Language · Theme
```

---

## 👨‍💻 Team

Built by ESPRIT engineering students as part of the **Java Desktop Development** module.

---

## 📄 License

This project is developed for academic purposes at **ESPRIT — École Supérieure Privée d'Ingénierie et de Technologies**, Tunisia.

---

<div align="center">

Made with ❤️ in Tunisia 🇹🇳

**[⭐ Star this repo](https://github.com/FyrasBenSlimane/Skilora-TN)** if Skilora inspired you!

</div>
