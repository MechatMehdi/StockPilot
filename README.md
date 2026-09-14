# StockPilot - Enterprise Inventory Management

[![Java 23](https://img.shields.io/badge/Java-23-orange.svg)](https://jdk.java.net/23/)
[![JavaFX 23](https://img.shields.io/badge/JavaFX-23-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/Database-SQLite-lightgrey.svg)](https://www.sqlite.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

StockPilot is a premium, production-grade desktop application for inventory and stock management. Built with JavaFX, SQLite, and modern UI design principles.

---

## 🌟 Overview & Key Features

- **Dashboard:** Real-time business metrics, analytics, and stock charts.
- **Catalogue:** Comprehensive product management supporting SKUs, pricing, categories, QR code generation, and high-res product images.
- **Stock Management:** Add, edit, archive products, and record stock adjustments defensively.
- **Activity Log & Audit:** Complete, audit-ready historical log of all stock movements and transactions.
- **Receipt & Invoice Export:** Automated PDF generation for customer transactions and inventory reports.
- **Premium UI:** Dark mode styling with CSS variables, responsive layout design, custom cell rendering, and smooth transitions.

---

## 🏗️ Architecture & Design Patterns

StockPilot is built following SOLID engineering principles and Clean Architecture:

- **Model-View-Controller (MVC):** Strict separation of UI rendering, domain logic, and state handlers.
- **Repository / Data Access Object (DAO):** Abstraction layer for transactional database persistence.
- **Singleton Pattern:** Controlled single database connection pool instance.
- **Strategy Pattern:** Dynamic sorting and multi-criteria catalogue filtering algorithms.
- **Composite Pattern:** Hierarchical structure for product catalog categories.
- **Factory Pattern:** Custom JavaFX cell rendering factories for enhanced grid performance.

---

## 💻 Tech Stack

- **Language:** Java 23+
- **UI Framework:** JavaFX 23
- **Database:** SQLite (via JDBC)
- **PDF Generation:** OpenPDF
- **QR Code Engine:** ZXing
- **Build System:** Apache Maven
- **Icons:** FontAwesomeFX

---

## ⚙️ Quick Start & Building from Source

### Prerequisites
- **JDK 23** or newer ([Download OpenJDK](https://jdk.java.net/23/))
- Maven wrapper included (`./mvnw` / `mvnw.cmd`)

### Clone & Run
```bash
# 1. Clone repository
git clone https://github.com/MechatMehdi/StockPilot.git
cd StockPilot

# 2. Run application locally
./mvnw.cmd clean javafx:run
```

### Build Executable JAR
```bash
# Package fat executable JAR
./mvnw.cmd clean package -DskipTests
```
The output executable JAR will be located at `target/StockPilot-1.0.jar`.

### Package Desktop Installer (Windows MSI)
```bash
./mvnw.cmd jlink:jlink
./mvnw.cmd jpackage:jpackage
```
The output installer (`StockPilot-1.0.msi`) will be generated under `target/dist/`.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

---

## 📄 License

Distributed under the [MIT License](LICENSE). Free for commercial and non-commercial open-source use.
