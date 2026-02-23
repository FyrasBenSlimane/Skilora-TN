import os
import shutil

# Config
PROJECT_ROOT = r"c:\Users\21625\Downloads\JAVAFX11\JAVAFX"
SOURCE_DIR = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "skilora", "controller")
TARGET_DIR = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "skilora", "recruitment", "controller")
FXML_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "com", "skilora", "view")

FILES_TO_MOVE = [
    "ActiveOffersController.java",
    "ApplicationInboxController.java",
    "ApplicationsController.java",
    "FeedController.java",
    "FormationsController.java",
    "InterviewsController.java",
    "JobDetailsController.java",
    "MyOffersController.java",
    "PostJobController.java",
    "ProfileWizardController.java"
]

# Ensure Target Dir
os.makedirs(TARGET_DIR, exist_ok=True)

print(f"Moving {len(FILES_TO_MOVE)} controllers to {TARGET_DIR}...")

for filename in FILES_TO_MOVE:
    source_path = os.path.join(SOURCE_DIR, filename)
    target_path = os.path.join(TARGET_DIR, filename)

    if not os.path.exists(source_path):
        print(f"Skipping {filename} (Not found)")
        continue

    # Read Content
    with open(source_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Update Package Declaration
    new_content = content.replace("package com.skilora.controller;", "package com.skilora.recruitment.controller;")

    # Write to New Location
    with open(target_path, "w", encoding="utf-8") as f:
        f.write(new_content)

    # Delete Old File
    os.remove(source_path)
    print(f"Moved {filename}")

print("Updating FXML references...")

# Update FXML References
if os.path.exists(FXML_DIR):
    for filename in os.listdir(FXML_DIR):
        if filename.endswith(".fxml"):
            fxml_path = os.path.join(FXML_DIR, filename)
            
            with open(fxml_path, "r", encoding="utf-8") as f:
                content = f.read()

            modified = False
            for class_name in FILES_TO_MOVE:
                class_base = class_name.replace(".java", "")
                old_ref = f"com.skilora.controller.{class_base}"
                new_ref = f"com.skilora.recruitment.controller.{class_base}"
                
                if old_ref in content:
                    content = content.replace(old_ref, new_ref)
                    modified = True
                    print(f"Updated {filename} reference to {class_base}")

            if modified:
                with open(fxml_path, "w", encoding="utf-8") as f:
                    f.write(content)

print("Refactoring Complete!")
