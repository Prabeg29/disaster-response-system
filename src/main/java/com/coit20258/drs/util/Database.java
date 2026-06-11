package com.coit20258.drs.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;

    private Database() {
    }

    public static void boot() {
        resolveConfig();

        try (Connection conn = getConnection()) {
            createTables(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DatabaseUtil.boot() failed: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null) {
            throw new IllegalStateException(
                    "Database has not been initialised. Call DatabaseUtil.boot() first.");
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    private static void resolveConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String host = dotenv.get("DB_HOST", "localhost");
        String port = dotenv.get("DB_PORT", "3306");
        String name = dotenv.get("DB_NAME", "drs_db");
        dbUser = dotenv.get("DB_USER", "root");
        dbPassword = dotenv.get("DB_PASS", "pass");

        // Extra JDBC options improve reliability:
        //   useSSL=false            — disable SSL for local dev (set to true in prod)
        //   allowPublicKeyRetrieval — needed for MySQL 8 caching_sha2_password
        //   serverTimezone          — avoids timezone negotiation warnings
        jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC&characterEncoding=UTF-8",
                host, port, name);
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // -----------------------------------------------------------------
            // 1. users
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                    + "  id            INT          NOT NULL AUTO_INCREMENT, "
                    + "  firstName     VARCHAR(50), "
                    + "  lastName      VARCHAR(50), "
                    + "  email         VARCHAR(150) NOT NULL UNIQUE, "
                    + "  passwordHash  VARCHAR(255) NOT NULL, " // plain SHA-256 hex per spec
                    + "  role          VARCHAR(255), "
                    + "  isActive      TINYINT(1)   NOT NULL DEFAULT 1, "
                    + "  createdAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  lastLoginAt   TIMESTAMP, "
                    + "  PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // -----------------------------------------------------------------
            // 2. disaster_reports
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS disaster_reports ("
                    + "  id             INT          NOT NULL AUTO_INCREMENT, "
                    + "  disasterType   VARCHAR(100) NOT NULL, "
                    + "  location       VARCHAR(255) NOT NULL, "
                    + "  severityLevel  VARCHAR(20)  NOT NULL, "
                    + "  description    TEXT, "
                    + "  status         VARCHAR(50)  NOT NULL DEFAULT 'REPORTED', "
                    + "  reportedById   INT          NOT NULL, "
                    + "  reportedAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  PRIMARY KEY (id), "
                    + "  CONSTRAINT fk_report_user FOREIGN KEY (reportedById) "
                    + "    REFERENCES users(id) ON DELETE RESTRICT"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // -----------------------------------------------------------------
            // 3. departments
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS departments ("
                    + "  id            INT          NOT NULL AUTO_INCREMENT, "
                    + "  name          VARCHAR(100) NOT NULL UNIQUE, "
                    + "  contactEmail  VARCHAR(150) NOT NULL, "
                    + "  contactPhone  VARCHAR(20)  NOT NULL, "
                    + "  isActive      TINYINT(1)   NOT NULL DEFAULT 1, "
                    + "  PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            stmt.executeUpdate(
                    "INSERT IGNORE INTO departments (name, contactEmail, contactPhone) VALUES "
                    + "('Fire & Rescue',          'fire@drs.gov',    '000'), "
                    + "('Medical Services',        'medical@drs.gov', '000'), "
                    + "('Police & Law Enforcement','police@drs.gov',  '000'), "
                    + "('Emergency Management',    'emergency@drs.gov','000'), "
                    + "('Public Works',            'works@drs.gov',   '000'), "
                    + "('Social Services',         'social@drs.gov',  '000'), "
                    + "('Environmental Agency',    'env@drs.gov',     '000')"
            );

            // -----------------------------------------------------------------
            // 4. department_updates   (depends on users, disaster_reports, departments)
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS department_updates ("
                    + "  id              INT          NOT NULL AUTO_INCREMENT, "
                    + "  reportId        INT          NOT NULL, "
                    + "  departmentId    INT          NOT NULL, "
                    + "  updatedById     INT          NOT NULL, "
                    + "  updateText      TEXT         NOT NULL, "
                    + "  responseStatus  VARCHAR(20)  NOT NULL DEFAULT 'RESPONDING', "
                    + "  updatedAt       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  PRIMARY KEY (id), "
                    + "  CONSTRAINT fk_du_report     FOREIGN KEY (reportId)     REFERENCES disaster_reports(id), "
                    + "  CONSTRAINT fk_du_department FOREIGN KEY (departmentId) REFERENCES departments(id), "
                    + "  CONSTRAINT fk_du_user       FOREIGN KEY (updatedById)  REFERENCES users(id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // Add departmentId column to users if it doesn't exist yet
            try {
                stmt.executeUpdate(
                        "ALTER TABLE users ADD COLUMN departmentId INT NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore duplicate column error (1060)
            }

            // Add notes column to evacuation_zones if it doesn't exist yet
            try {
                stmt.executeUpdate(
                        "ALTER TABLE evacuation_zones ADD COLUMN notes TEXT NULL");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore duplicate column error (1060)
            }

            // -----------------------------------------------------------------
            // 5. evacuation_zones   (depends on disaster_reports)
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS evacuation_zones ("
                    + "  id                INT          NOT NULL AUTO_INCREMENT, "
                    + "  name              VARCHAR(150) NOT NULL, "
                    + "  location          VARCHAR(255) NOT NULL, "
                    + "  capacity          INT          NOT NULL DEFAULT 0, "
                    + "  currentOccupancy  INT          NOT NULL DEFAULT 0, "
                    + "  status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', "
                    + "  reportId          INT          NOT NULL, "
                    + "  createdAt         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  PRIMARY KEY (id), "
                    + "  CONSTRAINT fk_evac_report FOREIGN KEY (reportId) "
                    + "    REFERENCES disaster_reports(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // -----------------------------------------------------------------
            // 6. disaster_assessments   (depends on users, disaster_reports)
            // -----------------------------------------------------------------
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS disaster_assessments ("
                    + "  id                        INT          NOT NULL AUTO_INCREMENT, "
                    + "  reportId                  INT          NOT NULL, "
                    + "  assessorId                INT          NOT NULL, "
                    + "  assessedSeverity          VARCHAR(20)  NOT NULL, "
                    + "  estimatedAffected         INT          NOT NULL DEFAULT 0, "
                    + "  isInfrastructureDamaged   TINYINT(1)   NOT NULL DEFAULT 0, "
                    + "  isHazardActive            TINYINT(1)   NOT NULL DEFAULT 0, "
                    + "  priorityScore             INT          NOT NULL DEFAULT 0, "
                    + "  recommendedActions        TEXT         NULL, "
                    + "  assignedDepartments       TEXT         NULL, "
                    + "  assessmentNotes           TEXT         NULL, "
                    + "  assessedAt                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "  PRIMARY KEY (id), "
                    + "  CONSTRAINT fk_assess_report "
                    + "    FOREIGN KEY (reportId)   REFERENCES disaster_reports(id), "
                    + "  CONSTRAINT fk_assess_assessor "
                    + "    FOREIGN KEY (assessorId) REFERENCES users(id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }
}
