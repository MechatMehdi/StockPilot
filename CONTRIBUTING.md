# Contributing to StockPilot

Thank you for your interest in contributing to **StockPilot**! We welcome contributions from developers of all skill levels to help make StockPilot the premier open-source desktop inventory management system.

---

## 🚀 How to Get Started

1. **Fork the Repository**: Click the **Fork** button at the top right of the GitHub repository.
2. **Clone your Fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/StockPilot.git
   cd StockPilot
   ```
3. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

---

## 🛠️ Build & Development Instructions

### Prerequisites
- **JDK 23** or higher ([OpenJDK Download](https://jdk.java.net/23/))
- **Maven** (included via `./mvnw.cmd` wrapper)

### Run & Build
```bash
# Run application locally
./mvnw.cmd clean javafx:run

# Package executable JAR
./mvnw.cmd clean package -DskipTests
```

---

## 📋 Pull Request Process

1. Ensure your code compiles cleanly without errors or warnings.
2. Write clear, descriptive commit messages adhering to standard format (e.g., `feat: add export to CSV feature`, `fix: resolve null pointer in catalogue filter`).
3. Push your branch to your fork and submit a **Pull Request** targeting the `main` branch.
4. Provide a detailed description in your PR of what was changed and why.

---

## 📜 Code Style & Quality Standards

- Maintain **Clean Architecture** and **MVC** pattern separation.
- Ensure strict type safety and defensive error handling (never leave empty catch blocks).
- Use clear variable and method names.
- Keep UI components responsive and styled cleanly with CSS variables.

---

## 📄 License

By contributing to StockPilot, you agree that your contributions will be licensed under the [MIT License](LICENSE).
