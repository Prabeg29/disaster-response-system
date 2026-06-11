# Disaster Response System — DRS Enhanced

A team-based university project (COIT20258 Software Engineering) built as a three-tier JavaFX desktop application backed by a multi-threaded TCP server and a MySQL database.

---

## What is this project about?

DRS Enhanced is a disaster response coordination platform that allows emergency staff to report disasters, assess their severity, coordinate department responses, manage evacuation zones, and track physical resources — all from a single desktop application. Every action is persisted in real time through an embedded TCP server to a MySQL database, ensuring data integrity across concurrent users.

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **User Authentication** | Register with a role (Admin, Responder, Department Coordinator) and log in with SHA-256 hashed credentials. Session is held in-memory for the duration of the session. |
| 2 | **Disaster Reporting** | Submit new disaster reports (type, location, severity, description). Filter reports by assigned department. Update report status inline from the table. |
| 3 | **Disaster Assessment** | Assess any report for severity, estimated affected people, infrastructure damage, and active hazards. A priority score is computed automatically. Assign recommended actions and departments. |
| 4 | **Priority Dashboard** | Read-only overview — live stat cards for total reports, critical, high severity, and resolved, plus a table of all assessments ranked by priority score. |
| 5 | **Department Coordination** | Department coordinators post status updates against reports. Updates are filtered by department and linked back to the originating report. |
| 6 | **Evacuation Zone Management** | Create evacuation zones linked to disaster reports. Track capacity and occupancy. Update zone status (ACTIVE / FULL / CLOSED). Edit or delete zones. |
| 7 | **Resource Management** | Track vehicles, equipment, personnel, medical supplies, and general supplies. Filter by type. Create, edit, and delete resources. Optionally link a resource to an active report. |
| 8 | **Audit Trail** | Every entity carries a timestamp column (`createdAt` / `updatedAt` / `assessedAt`). MySQL `DEFAULT CURRENT_TIMESTAMP` and `ON UPDATE CURRENT_TIMESTAMP` ensure server-side accuracy. |

---

## Project Flow Architecture

The application is a single JVM process that acts as both the TCP server and the JavaFX client simultaneously.

```
┌─────────────────────────────────────────────────────┐
│                  JavaFX Client (GUI)                │
│                                                     │
│   LoginView / RegisterView                          │
│        │                                            │
│   AppShellView + SidebarView                        │
│        │                                            │
│   ┌────┴────────────────────────────────────┐       │
│   │  Controllers (MVC)                      │       │
│   │  DashboardController                    │       │
│   │  DisasterReport{List,Form}Controller    │       │
│   │  DisasterAssessment{List,}Controller    │       │
│   │  DepartmentCoordinationController       │       │
│   │  EvacuationZoneController               │       │
│   │  ResourceController                     │       │
│   └────────────────┬───────────────────────-┘       │
│                    │  calls                         │
│             AppService (singleton TCP client)       │
└────────────────────┼────────────────────────────────┘
                     │  Java Object Serialisation
                     │  over TCP socket (localhost:9090)
┌────────────────────┼────────────────────────────────┐
│                    │  TCP Server                    │
│             DrsServer (daemon thread)               │
│          Fixed thread pool — 10 workers             │
│                    │                                │
│             ClientHandler (per connection)          │
│          Deserialises DrsRequest → dispatches       │
│          → serialises DrsResponse back              │
│                    │                                │
│   ┌────────────────┴───────────────────────┐        │
│   │  DAO Layer (one instance per handler)  │        │
│   │  UserDaoImpl                           │        │
│   │  DisasterReportDaoImpl                 │        │
│   │  DisasterAssessmentDaoImpl             │        │
│   │  DepartmentDaoImpl / UpdateDaoImpl     │        │
│   │  EvacuationZoneDaoImpl                 │        │
│   │  ResourceDaoImpl                       │        │
│   └────────────────┬───────────────────────┘        │
└────────────────────┼────────────────────────────────┘
                     │  JDBC
┌────────────────────┼────────────────────────────────┐
│            MySQL Database (drs_db)                  │
│   users · disaster_reports · disaster_assessments   │
│   departments · department_updates                  │
│   evacuation_zones · resources                      │
└─────────────────────────────────────────────────────┘
```

**Request lifecycle:**
1. A controller calls a method on `AppService`.
2. `AppService` opens a new `Socket`, serialises a `DrsRequest` (command string + args), sends it, and reads back a `DrsResponse`.
3. `DrsServer` accepts the connection and hands it to a `ClientHandler` thread from the pool.
4. `ClientHandler` switch-dispatches on the command string to the appropriate DAO method.
5. The DAO executes SQL against MySQL via JDBC and returns the result wrapped in `DrsResponse`.
6. `AppService` unwraps the response and returns the typed result to the controller.
7. The controller updates the JavaFX UI on the FX Application Thread via `Platform.runLater`.

---

## Project Folder Structure

```
drs/
├── pom.xml                          # Maven build — Java 21, JavaFX 21, MySQL Connector/J
├── dev.docker-compose.yml           # MySQL 8 container for local development
├── .env                             # Database credentials (not committed)
├── .env.example                     # Template for .env
│
└── src/main/
    ├── java/com/coit20258/drs/
    │   ├── Drs.java                 # JavaFX Application entry point
    │   │
    │   ├── model/                   # Plain serialisable domain objects (no enums)
    │   │   ├── User.java
    │   │   ├── DisasterReport.java
    │   │   ├── DisasterAssessment.java
    │   │   ├── Department.java
    │   │   ├── DepartmentUpdate.java
    │   │   ├── EvacuationZone.java
    │   │   └── Resource.java
    │   │
    │   ├── dao/                     # Data access — interface + Impl per entity
    │   │   ├── UserDao(Impl).java
    │   │   ├── DisasterReportDao(Impl).java
    │   │   ├── DisasterAssessmentDao(Impl).java
    │   │   ├── DepartmentDao(Impl).java
    │   │   ├── DepartmentUpdateDao(Impl).java
    │   │   ├── EvacuationZoneDao(Impl).java
    │   │   └── ResourceDao(Impl).java
    │   │
    │   ├── server/                  # Embedded TCP server
    │   │   ├── DrsServer.java       # ServerSocket on port 9090, thread pool
    │   │   ├── ClientHandler.java   # Per-connection worker, command dispatch
    │   │   ├── DrsRequest.java      # Serialisable command + args, CMD_* constants
    │   │   └── DrsResponse.java     # Serialisable result wrapper
    │   │
    │   ├── service/
    │   │   └── AppService.java      # Singleton TCP client — one method per command
    │   │
    │   ├── controller/              # JavaFX controllers (one per FXML view)
    │   │   ├── LoginController.java
    │   │   ├── RegisterController.java
    │   │   ├── AppShellController.java
    │   │   ├── SidebarController.java
    │   │   ├── DashboardController.java
    │   │   ├── DisasterReportListController.java
    │   │   ├── DisasterReportFormController.java
    │   │   ├── DisasterAssessmentListController.java
    │   │   ├── DisasterAssessmentController.java
    │   │   ├── DepartmentCoordinationController.java
    │   │   ├── EvacuationZoneController.java
    │   │   └── ResourceController.java
    │   │
    │   └── util/
    │       ├── Database.java        # JDBC connection pool, CREATE TABLE IF NOT EXISTS DDL
    │       ├── SceneManager.java    # JavaFX scene/view lifecycle manager
    │       ├── SessionContext.java  # Holds the logged-in User for the session
    │       └── Security.java        # SHA-256 password hashing
    │
    └── resources/com/coit20258/drs/
        ├── *.fxml                   # One FXML file per view
        └── styles/
            ├── drs-styles.css
            └── nav-styles.css
```

---

## Quick Setup

### Technologies

| Technology | Version |
|------------|---------|
| Java (JDK) | 17 or above |
| JavaFX | 21 |
| Maven | 3.8+ |
| MySQL | 8.x |
| MySQL Connector/J | 8.3.0 |
| dotenv-java | 3.2.0 |

---

### Setting Up the Database

**Option A — Docker (recommended)**

Requires Docker Desktop to be running.

```bash
# Linux / macOS
docker compose -f dev.docker-compose.yml up -d

# Windows (PowerShell)
docker compose -f dev.docker-compose.yml up -d
```

The container exposes MySQL on **port 4306** (mapped from the container's 3306). Set `DB_PORT=4306` in your `.env` when using Docker.

**Option B — Local MySQL**

Create the database manually. The application creates all tables automatically on first launch.

```sql
CREATE DATABASE drs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Providing the Environment File

Create a `.env` file in the project root (same folder as `pom.xml`):

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=drs_db
DB_USER=root
DB_PASS=your_password
```

> If using the Docker setup, set `DB_PORT=4306`.

All keys are optional — the application falls back to the defaults shown above if the file is missing.

---

### Running Tests

The project has a JUnit 5 unit test suite for `AppService`. The tests use a lightweight stub TCP server — no database or running application is needed.

```bash
# Linux / macOS / Windows
mvn test
```

Expected output:

```
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

#### What is tested

| Group | Tests |
|-------|-------|
| Auth | login (success + no match), register, emailExists (true + false) |
| Disaster Reports | findAll, findByDepartment, save, updateStatus |
| Assessments | findAll, findByReport (present + empty), save, update |
| Departments | findAll, findById (present + empty) |
| Department Updates | findByDepartment, findByReport, save |
| Evacuation Zones | findAll, findByReport, findById (present + empty), save, update, updateOccupancy, delete |
| Resources | findAll, findByType, findById (present + empty), save, update, delete |
| Error handling | server error response propagates as RuntimeException; connection refused propagates |

#### How the stub works

Each test starts a `StubDrsServer` on a random free OS port. The stub reads the serialised `DrsRequest`, passes it to a per-test lambda, and writes back the configured `DrsResponse`. `AppService.useEndpoint()` redirects the singleton to that port; `resetEndpoint()` restores the default after each test. No sockets leave `localhost` and no threads are leaked.

#### Manual smoke-test checklist

For end-to-end verification against a live database, run the application and exercise each feature:

1. Register a new user, log in — verify session loads.
2. Submit a disaster report — verify it appears in the list.
3. Click **Assess** on a report — fill the form and submit — verify the priority score updates on the dashboard.
4. Navigate to **Evacuation Zones** — create a zone, update its occupancy, then delete it.
5. Navigate to **Resources** — add a resource, filter by type, edit it, then delete it.
6. Log out — verify the session is cleared and the login screen is shown.

---

### Commands

#### Linux / macOS

```bash
# Compile
mvn compile

# Run the application
mvn javafx:run

# Clean build artifacts
mvn clean

# Package into a JAR
mvn package
```

#### Windows (Command Prompt / PowerShell)

```powershell
# Compile
mvn compile

# Run the application
mvn javafx:run

# Clean build artifacts
mvn clean

# Package into a JAR
mvn package
```

> Maven commands are identical on both platforms. Ensure `JAVA_HOME` points to JDK 21 and `mvn` is on your `PATH`.

**Verify your Java version:**

```bash
# Linux / macOS
java -version

# Windows
java -version
```

Expected output: `openjdk version "21.x.x"` or similar.

---

### Setup Using NetBeans

1. **Open the project**
   - Launch NetBeans (17 or later recommended).
   - Go to **File → Open Project**.
   - Navigate to the `drs/` folder and click **Open Project**. NetBeans detects the `pom.xml` and loads it as a Maven project.

2. **Set the JDK**
   - Right-click the project in the Projects panel → **Properties → Build → Compile**.
   - Set **Java Platform** to JDK 17. If it is not listed, go to **Tools → Java Platforms → Add Platform** and point it to your JDK 17 installation.

3. **Create the `.env` file**
   - In the Files panel, right-click the project root → **New → Empty File** → name it `.env`.
   - Paste in your database credentials (see [Providing the Environment File](#providing-the-environment-file) above).

4. **Start the database**
   - Run `docker compose -f dev.docker-compose.yml up -d` in a terminal, or start your local MySQL server.

5. **Run the application**
   - Click the **Run Project** button (▶) or press **F6**.
   - NetBeans executes `mvn javafx:run`. The application window opens; tables are created automatically on first run.

6. **Clean and build**
   - Right-click the project → **Clean and Build** to compile and package.
   - The output JAR is placed in `target/`.
