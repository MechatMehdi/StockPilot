# StockPilot - Enterprise Inventory Management

StockPilot is a premium, production-grade desktop application for inventory and stock management. Built with JavaFX, SQLite, and modern UI principles.

## Installation

### Windows (Recommended)
1. Download the latest `StockPilot-1.0.msi` from [Releases](../../releases)
2. Run the installer
3. StockPilot will be installed and available in Start Menu

### Requirements
- Windows 10 or later
- No additional software required (JRE included)

## Features
- **Dashboard:** Real-time metrics and charts.
- **Catalogue:** Manage products with images, SKUs, and pricing.
- **Stock Management:** Add, edit, archive products and adjust stock.
- **Activity Log:** Complete history of all stock changes.
- **Premium UI:** Dark mode with CSS variables and modern animations.

## Tech Stack
- Java 23
- JavaFX 25
- SQLite (via JDBC)
- Maven
- FontAwesomeFX

## Design Patterns Used
- Model-View-Controller (MVC)
- Repository / Data Access Object (DAO)
- Singleton (Database Connection)
- Strategy (Sorting & Filtering)
- Composite (Product Catalog)
- Factory (Cell Renderers)

## Development

### Prerequisites
- Java 23+ ([Download OpenJDK](https://jdk.java.net/23/))
- Maven (included via mvnw wrapper)

### Building from Source
```bash
cd gl_project
./mvnw.cmd clean package -DskipTests
```

### Creating an Installer
```bash
cd gl_project
./mvnw.cmd jlink:jlink
./mvnw.cmd jpackage:jpackage
```

The installer will be created in `gl_project/target/dist/`

## License
MIT License - feel free to use in personal and commercial projects

## Contributing
Contributions are welcome! Please feel free to submit issues and pull requests.
