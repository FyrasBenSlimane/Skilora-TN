# Skilora Project Structure

## Overview
This document describes the project's file structure after the comprehensive cleanup and FXML migration.

## Directory Structure

```
src/main/
├── java/com/skilora/
│   ├── Main.java                          # Application entry point
│   │
│   ├── config/                            # Configuration classes
│   │   ├── AppConfig.java                 # Application settings
│   │   ├── DatabaseConfig.java            # Database connection
│   │   └── DatabaseInitializer.java       # DB schema setup
│   │
│   ├── controller/                        # FXML Controllers (MVC)
│   │   ├── BiometricAuthController.java   # Biometric auth logic
│   │   ├── DashboardController.java       # Dashboard logic (New)
│   │   ├── FeedController.java            # Job feed logic
│   │   ├── LoginController.java           # Login logic
│   │   ├── MainController.java            # Main navigation logic
│   │   ├── ProfileWizardController.java   # Profile editing logic (New)
│   │   └── RegisterController.java        # Registration logic
│   │
│   ├── framework/                         # Reusable UI Framework
│   │   ├── components/                    # TL* Custom Components
│   │   │   ├── TLButton.java
│   │   │   └── ...
│   │   ├── layouts/
│   │   │   └── TLAppLayout.java
│   │   └── ...
│   │
│   ├── model/                             # Data layer
│   │   ├── entity/                        # Entity classes
│   │   ├── enums/                         # Enum types
│   │   └── service/                       # Business logic services
│   │
│   ├── ui/                                # UI-specific code
│   │   └── views/                         # View Wrappers / Complex Views
│   │       ├── MainView.java              # Main Layout Manager (Loads FXMLs)
│   │       └── ProfileWizardView.java     # Wrapper for Profile FXML
│   │
│   └── view/                              # (Empty/Removed - all FXML in resources)
│
└── resources/com/skilora/
    ├── assets/
    │   └── videos/
    ├── ui/
    │   ├── fonts/
    │   └── styles/
    │
    └── view/                              # FXML View definitions
        ├── BiometricAuthDialog.fxml       # Face verification UI
        ├── DashboardView.fxml             # Dashboard UI (New)
        ├── FeedView.fxml                  # Job feed UI
        ├── LoginView.fxml                 # Login UI
        ├── MainView.fxml                  # (Placeholder for future)
        ├── ProfileWizardView.fxml         # Profile editing UI (New)
        └── RegisterView.fxml              # Registration UI
```

## FXML Migration Status

### ✅ Fully Migrated to FXML
| View | FXML File | Controller |
|------|-----------|------------|
| Login | `LoginView.fxml` | `LoginController.java` |
| Register | `RegisterView.fxml` | `RegisterController.java` |
| Feed | `FeedView.fxml` | `FeedController.java` |
| Dashboard | `DashboardView.fxml` | `DashboardController.java` |
| Profile | `ProfileWizardView.fxml` | `ProfileWizardController.java` |
| Biometric | `BiometricAuthDialog.fxml` | `BiometricAuthController.java` |

### ⚠️ Partially Migrated (Hybrid)
- `MainView.java`: Acts as the main layout manager. It builds the sidebar/menu programmatically (due to role complexity) but loads `DashboardView.fxml`, `ProfileWizardView.fxml`, etc. for the center content.

## Architecture Pattern

The project follows **MVC (Model-View-Controller)**:

- **Model**: `model/entity/`, `model/service/`
- **View**: `resources/com/skilora/view/*.fxml`
- **Controller**: `controller/*Controller.java`

## Key Design Decisions

1. **Sub-View Pattern**: `MainView` handles the shell (Chrome), but delegates content areas to separate FXML files (`DashboardView`, `ProfileWizardView`, `FeedView`).
2. **Direct FXML Loading**: `FXMLLoader` is used for all major views.
3. **Controller Factories**: Complex controllers (like `BiometricAuthController`) use static factory methods (`showDialog`) to encapsulate FXML loading and stage management.

## Running the Application

```bash
mvn clean javafx:run
```
