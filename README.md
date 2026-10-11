# 📚 Athenaeum CampusLib Exchange — Collegiate Library Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20%2F%2017-orange.svg)](https://www.oracle.com/java/)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT%20Stateless-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-H2%20JPA%20Hibernate-lightgrey.svg)](https://www.h2database.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An enterprise collegiate inter-branch library exchange and circulation management system. CampusLib unites campus branch libraries into a single unified catalog, enabling automated inter-branch book courier dispatch, real-time delivery tracking, pessimistic locking for concurrency-safe checkouts, and 48-hour pickup hold shelf reservations.

---

## 🌟 Key Features

### 🎓 1. Student Portal & Live Courier Tracker
- **Unified 3-Branch Catalog:** Search and filter books across **Central Library**, **Turing CS Library**, and **Franklin Mechanical Library**.
- **Inter-Branch Courier Transfers:** Request books located at another campus branch with automated transfer routing.
- **Visual Delivery Stepper:** Track transfer stages in real-time (`REQUESTED` ➔ `DISPATCHED` ➔ `READY_FOR_PICKUP` ➔ `COMPLETED`).
- **Active Loans & Due Dates:** Monitor borrowed books with return dates and overdue alerts.

### 💻 2. Librarian Desk & Circulation Hub
- **Branch Dispatch Desk:** Review incoming transit requests and dispatch courier parcels across branches.
- **Pickup Hold Shelf:** Receive arriving couriers and place books on a **48-hour pickup hold shelf** with live expiry countdowns.
- **Quick Circulation Checkout Terminal:** 1-Click hold issuing or direct barcode scanning (`BC-CS-301`, etc.) with automatic patron resolution.
- **Returns Processing:** Check in returned books to immediately restore campus inventory.

### 🛡️ 3. Administrative Audit & Inventory Ledger
- **Live Audit Trail:** Complete tamper-evident event log of every catalog request, status transition, checkout, and return.
- **Branch Inventory Monitoring:** Real-time stock counts across all departments.

### 🔐 4. Authentication & Security
- **JWT (JSON Web Token) Stateless Authentication:** Secure session management with role-based endpoint permissions.
- **Self-Registration:** Custom student identity creation with personalized University IDs (`STU-...`).
- **Fast-Pass Demo Personas:** Switch between pre-seeded personas (Student, CS Librarian, Central Librarian, Admin) in 1 click.
- **Persistent Student Sessions:** Switch between Librarian dispatch desks and student views without needing to log in repeatedly.

---

## 🛠️ Tech Stack & Architecture

- **Backend:** Java 17/21, Spring Boot 3.2.3
- **Persistence:** Spring Data JPA, Hibernate, H2 In-Memory Database
- **Concurrency Control:** Pessimistic write locks (`PessimisticLockScope.NORMAL`) to prevent race conditions during book checkout.
- **Security:** Spring Security 6, JWT, BCrypt Password Encoder (strength 12)
- **Frontend:** Responsive SPA using Vanilla JavaScript, HTML5, Tailwind CSS, Google Material Symbols, and an HTML5 Canvas hero animation player.

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or 21+ installed and configured on your `PATH`.
- **Git**

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Bhavesh1424/Library-Management.git
   cd Library-Management
   ```

2. **Run the Spring Boot application:**
   - **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. **Open the application in your browser:**
   ```
   http://localhost:8080
   ```

---

## 👥 Pre-Seeded Demo Accounts

You can use the **⚡ Demo Access** tab on the login screen to test immediately, or log in with these credentials:

| Role | Name | Email / Login ID | Password | Department / Branch |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | Aarav Sharma | `student@campus.edu` | `password123` | CS Dept (`STU-2024-001`) |
| **Librarian** | Priya Nair | `cs.librarian@campus.edu` | `password123` | Computer Science Desk (`CS-DEPT`) |
| **Librarian** | Rajesh Kumar | `central.librarian@campus.edu` | `password123` | Central Library Desk (`CENTRAL`) |
| **Admin** | Chief Admin | `admin@campus.edu` | `admin123` | Campus System Administrator |

---

## 📡 REST API Endpoints Overview

### Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Register a new student profile
- `POST /api/v1/auth/login` — Sign in and receive JWT token
- `GET /api/v1/auth/me` — Retrieve active authenticated user profile

### Catalog & Inventory (`/api/v1/books`)
- `GET /api/v1/books` — Search books by keyword, author, branch, or availability
- `GET /api/v1/books/{id}` — Get book details and all physical copies across branches

### Inter-Branch Transfers (`/api/v1/transfers`)
- `POST /api/v1/transfers/request` — Place an inter-branch transfer request
- `GET /api/v1/transfers/my-requests` — Get active transfer orders for the logged-in student
- `GET /api/v1/transfers/pending-dispatch` — List outgoing transfers awaiting librarian dispatch
- `PUT /api/v1/transfers/{id}/dispatch` — Mark transfer parcel as dispatched
- `PUT /api/v1/transfers/{id}/receive` — Receive transfer and place on 48h hold shelf
- `GET /api/v1/transfers/hold-shelf` — List books currently on the pickup hold shelf

### Circulation & Loans (`/api/v1/loans`)
- `POST /api/v1/loans/checkout` — Issue a book loan (with pessimistic locking)
- `POST /api/v1/loans/{id}/return` — Return a borrowed book copy
- `GET /api/v1/loans/my-loans` — List active loans for current student
- `GET /api/v1/loans/active` — List all campus-wide active loans (Librarian/Admin)
- `GET /api/v1/loans/students` — Fetch registered student patrons for checkout dropdown

### Audit & Administration (`/api/v1/admin`)
- `GET /api/v1/admin/audit-logs` — Fetch chronological audit logs
- `GET /api/v1/branches` — Retrieve campus branch directory

---

## 🏛️ Project Directory Structure

```
├── src/
│   ├── main/
│   │   ├── java/com/campus/library/
│   │   │   ├── config/          # SecurityConfig, JWTFilter, DataInitializer
│   │   │   ├── controller/      # REST API Controllers
│   │   │   ├── dto/             # Request & Response Records
│   │   │   ├── entity/          # JPA Entities (Book, Copy, Transfer, Loan, User)
│   │   │   ├── repository/      # Spring Data JPA Repositories
│   │   │   └── service/         # Business Logic & Circulation Services
│   │   └── resources/
│   │       ├── application.yml  # Database, JWT & Server Configuration
│   │       └── static/          # Single Page Application (HTML, CSS, JS, Frames)
│   └── test/                    # Unit & Integration Tests
├── pom.xml                      # Maven Dependencies & Plugins
└── README.md                    # Project Documentation
```

---

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.
