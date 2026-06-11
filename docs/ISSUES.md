# DRS — Open Issues

Identified by comparing `Software-3.pdf` against the current codebase.

---

## HIGH

### 1. No role-based access control on the sidebar
**File:** `src/main/java/com/coit20258/drs/controller/SidebarController.java`

All six navigation buttons are wired unconditionally. A `DEPARTMENT` coordinator can still click **Reports**, **Assessments**, **Dashboard**, etc. after login. The spec explicitly requires "access rights to view and modify data."

**Fix:** Call `applyRoleAccess(role)` from `populateUserInfo()` and hide/disable buttons based on `SessionContext.getCurrentUser().getRole()`:
- `DEPARTMENT` → show only `navDeptCoordination`
- `REPORTER` → hide `navAssessments`, `navDashboard`, `navDeptCoordination`
- `OPERATOR` / `ADMIN` → full access

---

## MEDIUM

### 2. Missing departments from specification
**Files:** `src/main/java/com/coit20258/drs/util/Database.java` (line 110), `seed.sql`

The PDF spec lists eight required external organizations to interface with. The current seeded departments cover only three of them directly. Missing:

| Required by spec | Status |
|---|---|
| Fire and emergency services | ✓ (Fire & Rescue) |
| Hospital | ✓ (Medical Services) |
| Law enforcement | ✓ (Police & Law Enforcement) |
| Electricity | ✗ missing |
| Transportation | ✗ missing |
| Waste management | ✗ missing |
| Water supply | ✗ missing |
| Schools | ✗ missing |

**Fix:** Add the five missing rows to the `INSERT IGNORE INTO departments` block in both `Database.java` and `seed.sql`.

---

### 3. "Encryption and decryption" requirement not met
**File:** `src/main/java/com/coit20258/drs/util/Security.java`

The spec requires *"Encryption and decryption."* The current implementation uses SHA-256, which is one-way hashing — not reversible encryption. This satisfies password storage but does not technically satisfy the stated requirement.

**Fix options:**
1. Add AES symmetric encryption (using `javax.crypto`) to a sensitive field (e.g. disaster `description`) to demonstrate both encrypt and decrypt.
2. At minimum, document in the report why SHA-256 is used for passwords and add even a small utility method demonstrating AES round-trip for grading purposes.

---

### 4. Dead DAO methods not wired to the TCP server
The following methods are fully implemented in SQL but have no `CMD_*` constant, no `AppService` method, and no `ClientHandler` case — they are unreachable from any controller:

| Method | File | Purpose |
|---|---|---|
| `UserDaoImpl.findByEmail()` | `dao/UserDaoImpl.java:105` | Look up user by email |
| `UserDaoImpl.setActiveStatus()` | `dao/UserDaoImpl.java:67` | Activate / deactivate a user account |
| `DisasterReportDaoImpl.findById()` | `dao/DisasterReportDaoImpl.java:84` | Fetch a single report |
| `DisasterReportDaoImpl.findByReporterId()` | `dao/DisasterReportDaoImpl.java:104` | Filter reports by who filed them |

**Fix:** Either wire them fully (add CMD constant → `ClientHandler` case → `AppService` method → controller usage) or document them as internal-only utilities in the report.

---

## LOW

### 5. No disaster report edit feature
**Files:** `controller/DisasterReportListController.java`, `dao/DisasterReportDaoImpl.java`

Only the `status` field can be changed on an existing report. There is no way to edit `disasterType`, `location`, `severityLevel`, or `description`, and `DisasterReportDaoImpl` has no `update()` method.

**Fix:** Add `CMD_REPORTS_UPDATE`, a `DisasterReportDao.update()` interface + impl, a `ClientHandler` case, an `AppService.updateReport()` method, and an edit path in the report list view (mirroring the pattern in `EvacuationZoneController`).

---

### 6. Severity dropdown order is wrong in the report form
**File:** `src/main/java/com/coit20258/drs/controller/DisasterReportFormController.java` (line ~144)

Current order: `CRITICAL, HIGH, LOW, MEDIUM`
Should be: `CRITICAL, HIGH, MEDIUM, LOW`

---

### 7. Dashboard shows orphaned assessments
**File:** `src/main/java/com/coit20258/drs/controller/DashboardController.java` (line 75)

Assessments whose linked `DisasterReport` is `null` are still displayed in the priority table, showing `"—"` in the type and location columns instead of being excluded.

**Fix:**
```java
.filter(a -> a.getDisasterReport() != null)
```
Add this before the `.sorted(...)` in the `loadData()` stream.
