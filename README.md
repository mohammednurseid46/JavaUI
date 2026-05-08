# OCES — Online Course Enrollment System

> A full-featured desktop Learning Management System built with Java 17 and JavaFX 21.

---

## 📌 Overview

OCES (Online Course Enrollment System) is a standalone Windows desktop application that provides a complete platform for managing online course enrollments. It supports three user roles — **Admin**, **Instructor**, and **Student** — each with a dedicated interface and scoped access.

All data is stored locally in a SQLite database (`lms_database.db`). No internet connection or external database server is required.

---

## ✨ Features

### Admin
- Full dashboard with live statistics (students, courses, enrollments, revenue, completion rate)
- Course management — create, edit, delete, assign instructors
- Student management — register, edit, activate/deactivate, manage enrollments
- Instructor management — register, activate/deactivate, reset passwords
- Enrollment management — enroll/drop students, update status, filter and sort
- Reports — PDF, Excel, Word export (accreditation, financial, academic progress, grade book, certificates)
- Import/Export — bulk import students/courses/enrollments from CSV
- Password management — view, reset, generate temp passwords, BCrypt demo, password history
- Analytics dashboard with monthly enrollment trend chart and live activity log

### Instructor
- Personal dashboard with course statistics
- View enrolled students per course
- Assign and update grades (0–100)
- Mark enrollment status (Enrolled / Completed / Dropped)
- Analytics — pie chart, grade distribution bar chart, grading progress
- Report generation

### Student
- Browse all active courses and enroll directly
- View enrollment history with status and grades
- View grades with bar chart visualization and GPA
- Edit personal profile and change password
- Self-registration via Sign Up page

---

## 🛠️ Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| UI Framework | JavaFX | 21 |
| Database | SQLite (via JDBC) | 3.44.1.0 |
| Build Tool | Apache Maven | 3.6+ |
| Password Hashing | jBCrypt | 0.4 |
| PDF Generation | iText7 | 7.2.5 |
| Excel Generation | Apache POI OOXML | 5.2.5 |
| Word Generation | Apache POI Scratchpad | 5.2.5 |
| CSV Processing | OpenCSV | 5.9 |
| Charts | XChart | 3.8.8 |
| Packaging | Maven Shade Plugin (fat JAR) | 3.5.1 |

---

## 🗄️ Database Schema

The application uses **8 tables** created automatically on first run:

| Table | Purpose |
|---|---|
| `users` | All users (admin, instructor, student) with BCrypt password hash |
| `students` | Student academic profiles linked to users |
| `instructors` | Instructor professional profiles linked to users |
| `courses` | Course catalog with capacity, credits, price, status |
| `enrollments` | Student–course relationships with status and grade |
| `grades` | Individual assignment grades per enrollment |
| `payments` | Payment records per enrollment |
| `course_evaluations` | Student ratings and feedback for courses |
| `password_history` | Full audit trail of every password change (BCrypt hash stored) |

---

## 🚀 Quick Start

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.6+

### Run from Source

```bash
# Clone or open the project
cd OnlineCourseEnrollmentSystem

# Compile
mvn compile

# Run
mvn javafx:run
```

### Build Portable JAR

```bash
mvn package
# Output: target/OCES-installer/OCES.jar
```

### Run Portable JAR

```cmd
java -Djava.awt.headless=false -jar target\OCES-installer\OCES.jar
```

### Build Windows Installer

```cmd
build-installer.bat
# Output: target/installer-output/OCES-1.0.0.exe
```

---

## 👥 Default Login Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Instructor | set by admin | `Instructor@123` (default) |
| Student | chosen at signup | set during registration |

> Students can self-register via the **Sign Up** page on the login screen.

---

## 🖥️ System Tray

Once running, OCES lives in the Windows system tray:

| Action | Result |
|---|---|
| Close window (X) | Minimizes to tray — keeps running |
| Double-click tray icon | Restores the window |
| Right-click tray icon | Shows: Open / Hide / Exit |
| Exit OCES | Fully closes the application |

---

## 📁 Project Structure

```
src/main/java/com/lms/analytics/
├── App.java                    ← Entry point wrapper
├── Louncher.java               ← Main JavaFX Application
├── controllers/                ← 22 UI controllers
├── dao/                        ← 6 Data Access Objects + PasswordHistoryDAO
├── dto/                        ← 4 Analytics DTOs
├── models/                     ← 7 Domain models + PasswordHistory
├── services/                   ← 6 Business logic services
└── utils/                      ← 12 Utility classes

src/main/resources/
├── config/database.properties  ← SQLite connection config
├── css/                        ← 4 CSS stylesheets (light + dark theme)
├── fxml/                       ← 22 FXML view files
└── images/                     ← Logo and background images
```

---

## 🔐 Security

- Passwords hashed with **BCrypt** (workload factor 12)
- All SQL queries use **PreparedStatement** (no SQL injection)
- Role-based access control — Admin / Instructor / Student
- Password history table stores every BCrypt hash with audit trail
- Password reuse prevention (last 3 passwords checked)
- Strong password validation: min 8 chars, uppercase, lowercase, digit, special char
- Email format validation on all registration forms

---

## 🛠️ Troubleshooting

**App does not start:**
- Ensure JDK 17+ is installed: `java -version`
- Ensure `java.awt.headless=false` is set

**System tray icon not showing:**
- Windows → Taskbar Settings → System Tray Icons → Enable for OCES

**Database not found:**
- `lms_database.db` is auto-created on first run in the same folder as the JAR

**Registration fails:**
- Check that the email format is valid (e.g. `user@domain.com`)
- Password must meet strength requirements: 8+ chars, mixed case, number, symbol

**Reports not generating:**
- Ensure the save directory is writable
- For student/instructor reports, select a name from the searchable dropdown first

---

## 📋 Developed By

**Mohammednur Seid**

**OCES v2025 — Online Course Enrollment System**
