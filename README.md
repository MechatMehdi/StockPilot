# StockPilot - Enterprise Inventory Management

[![Java 23](https://img.shields.io/badge/Java-23-orange.svg)](https://jdk.java.net/23/)
[![JavaFX 25](https://img.shields.io/badge/JavaFX-25-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/Database-SQLite-lightgrey.svg)](https://www.sqlite.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)

StockPilot is a premium, production-grade desktop application for inventory and stock management. Built with JavaFX, SQLite, and modern UI design principles.

---

## 🌟 Overview & Key Features

- **Dashboard:** Real-time business metrics, analytics, and stock charts.
- **Catalogue:** Comprehensive product management supporting SKUs, pricing, categories, and high-res images.
- **Stock Management:** Add, edit, archive products, and record stock adjustments defensively.
- **Activity Log:** Complete, audit-ready historical log of all stock movements.
- **Premium UI:** Dark mode styling with CSS variables, responsive layout design, and smooth transitions.

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
- **UI Framework:** JavaFX 25
- **Database:** SQLite (via JDBC)
- **Build System:** Apache Maven
- **Icons:** FontAwesomeFX

---

## ⚙️ Development & Building

### Prerequisites
- JDK 23 or newer ([Download OpenJDK](https://jdk.java.net/23/))
- Maven (included via standard `mvnw.cmd` wrapper)

### Building from Source
```bash
# Navigate to project root
cd gl_project

# Compile and package executable JAR
./mvnw.cmd clean package -DskipTests
```

### Packaging Desktop Installer
```bash
# Create runtime image and installer bundle
./mvnw.cmd jlink:jlink
./mvnw.cmd jpackage:jpackage
```

The output installer (`StockPilot-1.0.msi`) will be generated under `gl_project/target/dist/`.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the issues page or submit a pull request targeting `main`.

## 📄 License
Distributed under the [MIT License](LICENSE).

