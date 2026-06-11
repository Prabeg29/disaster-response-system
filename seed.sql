-- =============================================================================
-- DRS-Enhanced — Database seed script
-- =============================================================================
-- Creates the schema and populates representative sample data so the
-- application can be demonstrated without registering anything manually.
--
-- Usage:
--   mysql -u root -p < seed.sql
--   (or paste into MySQL Workbench / DBeaver and execute)
--
-- All seed user accounts share the password:  password
-- SHA-256("password") = 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
-- =============================================================================

CREATE DATABASE IF NOT EXISTS drs_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE drs_db;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. users
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            INT          NOT NULL AUTO_INCREMENT,
    firstName     VARCHAR(50),
    lastName      VARCHAR(50),
    email         VARCHAR(150) NOT NULL UNIQUE,
    passwordHash  VARCHAR(255) NOT NULL,
    role          VARCHAR(255),
    isActive      TINYINT(1)   NOT NULL DEFAULT 1,
    departmentId  INT          NOT NULL DEFAULT 0,
    createdAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastLoginAt   TIMESTAMP    NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- password = "password"  (SHA-256 hex, no salt)
INSERT IGNORE INTO users (firstName, lastName, email, passwordHash, role, departmentId) VALUES
    ('System',  'Admin',    'admin@drs.gov',        '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'ADMIN',       0),
    ('John',    'Smith',    'john.smith@drs.gov',   '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'REPORTER',    0),
    ('Sarah',   'Jones',    'sarah.jones@drs.gov',  '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'REPORTER',    0),
    ('Mike',    'Chen',     'mike.chen@drs.gov',    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'OPERATOR',    0),
    ('Lisa',    'Park',     'lisa.park@drs.gov',    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DEPARTMENT',  1),
    ('David',   'Nguyen',   'david.nguyen@drs.gov', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'DEPARTMENT',  2);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. departments
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id            INT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL UNIQUE,
    contactEmail  VARCHAR(150) NOT NULL,
    contactPhone  VARCHAR(20)  NOT NULL,
    isActive      TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO departments (name, contactEmail, contactPhone) VALUES
    ('Fire & Rescue',           'fire@drs.gov',        '000'),
    ('Medical Services',        'medical@drs.gov',     '000'),
    ('Police & Law Enforcement','police@drs.gov',       '000'),
    ('Emergency Management',    'emergency@drs.gov',   '000'),
    ('Public Works',            'works@drs.gov',       '000'),
    ('Social Services',         'social@drs.gov',      '000'),
    ('Environmental Agency',    'env@drs.gov',         '000');

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. disaster_reports
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS disaster_reports (
    id             INT          NOT NULL AUTO_INCREMENT,
    disasterType   VARCHAR(100) NOT NULL,
    location       VARCHAR(255) NOT NULL,
    severityLevel  VARCHAR(20)  NOT NULL,
    description    TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'REPORTED',
    reportedById   INT          NOT NULL,
    reportedAt     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_user FOREIGN KEY (reportedById) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO disaster_reports (id, disasterType, location, severityLevel, description, status, reportedById, reportedAt) VALUES
    (1, 'FLOOD',      'Brisbane CBD, QLD',       'CRITICAL', 'Severe flooding across the central business district following record rainfall. Multiple roads cut off and buildings submerged to ground floor.', 'UNDER_ASSESSMENT', 2, '2026-06-01 08:15:00'),
    (2, 'FIRE',       'Western Sydney, NSW',      'HIGH',     'Bushfire spreading towards residential areas. Approximately 200 properties at risk. Fire front moving east due to strong winds.', 'RESPONDING',       3, '2026-06-02 14:30:00'),
    (3, 'EARTHQUAKE', 'Melbourne East, VIC',      'CRITICAL', 'Magnitude 5.8 earthquake. Significant structural damage reported in eastern suburbs. Multiple buildings collapsed. Gas leaks detected.', 'RESPONDING',       2, '2026-06-03 06:45:00'),
    (4, 'STORM',      'Gold Coast, QLD',          'MEDIUM',   'Category 2 cyclone causing widespread storm damage. Power outages affecting 15,000 residents. Coastal flooding expected.', 'REPORTED',         3, '2026-06-04 11:00:00'),
    (5, 'LANDSLIDE',  'Blue Mountains, NSW',      'HIGH',     'Major landslide blocking highway and isolating two townships. Residents requested to shelter in place pending road clearance.', 'RESOLVED',         2, '2026-05-28 09:20:00');

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. disaster_assessments
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS disaster_assessments (
    id                      INT          NOT NULL AUTO_INCREMENT,
    reportId                INT          NOT NULL,
    assessorId              INT          NOT NULL,
    assessedSeverity        VARCHAR(20)  NOT NULL,
    estimatedAffected       INT          NOT NULL DEFAULT 0,
    isInfrastructureDamaged TINYINT(1)   NOT NULL DEFAULT 0,
    isHazardActive          TINYINT(1)   NOT NULL DEFAULT 0,
    priorityScore           INT          NOT NULL DEFAULT 0,
    recommendedActions      TEXT         NULL,
    assignedDepartments     TEXT         NULL,
    assessmentNotes         TEXT         NULL,
    assessedAt              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_assess_report    FOREIGN KEY (reportId)   REFERENCES disaster_reports(id),
    CONSTRAINT fk_assess_assessor  FOREIGN KEY (assessorId) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Priority score formula: severity + min((affected/100)*5, 30) + 15*(infra) + 15*(hazard)
-- Report 1 (FLOOD, CRITICAL): 40 + min((500/100)*5,30) + 15 + 15 = 40+25+15+15 = 95
-- Report 2 (FIRE,  HIGH):     30 + min((150/100)*5,30) +  0 +  0 = 30+5        = 35
-- Report 3 (QUAKE, CRITICAL): 40 + min((1000/100)*5,30)+ 15 + 15 = 40+30+15+15 = 100
INSERT IGNORE INTO disaster_assessments (id, reportId, assessorId, assessedSeverity, estimatedAffected, isInfrastructureDamaged, isHazardActive, priorityScore, recommendedActions, assignedDepartments, assessmentNotes, assessedAt) VALUES
    (1, 1, 4, 'CRITICAL', 500,  1, 1,  95, 'Immediate evacuation of low-lying zones;Deploy water pumping equipment;Coordinate emergency shelters', '1,2,4', 'Flooding is expected to worsen over the next 24 hours. Priority is civilian evacuation and road reopening.', '2026-06-01 10:00:00'),
    (2, 2, 4, 'HIGH',     150,  0, 0,  35, 'Establish firebreak perimeter;Evacuate at-risk properties;Monitor wind direction', '1,3',   'Fire is currently contained to bushland. Residential evacuation is precautionary.', '2026-06-02 16:00:00'),
    (3, 3, 4, 'CRITICAL', 1000, 1, 1, 100, 'Search and rescue in collapsed buildings;Isolate gas leaks;Structural safety inspections;Set up triage centres', '1,2,3,4', 'Multiple building collapses confirmed. Aftershocks possible. Gas network shut-off requested from utility provider.', '2026-06-03 08:30:00');

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. evacuation_zones
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS evacuation_zones (
    id               INT          NOT NULL AUTO_INCREMENT,
    name             VARCHAR(150) NOT NULL,
    location         VARCHAR(255) NOT NULL,
    capacity         INT          NOT NULL DEFAULT 0,
    currentOccupancy INT          NOT NULL DEFAULT 0,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    reportId         INT          NOT NULL,
    notes            TEXT         NULL,
    createdAt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_evac_report FOREIGN KEY (reportId) REFERENCES disaster_reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO evacuation_zones (id, name, location, capacity, currentOccupancy, status, reportId, notes, createdAt) VALUES
    (1, 'Brisbane City Hall Shelter',  'Brisbane City Hall, King George Square, QLD',    500, 342, 'ACTIVE', 1, 'Primary evacuation point. Cots and catering provided. Medical team on site.', '2026-06-01 09:00:00'),
    (2, 'Southbank Convention Centre', 'South Bank Parklands, Grey St, South Brisbane',  800, 800, 'FULL',   1, 'At maximum capacity. Redirect new arrivals to RNA Showgrounds.', '2026-06-01 11:30:00'),
    (3, 'RNA Showgrounds Emergency',   'RNA Showgrounds, Bowen Hills, QLD',             1200, 210, 'ACTIVE', 1, 'Secondary overflow site. Pet-friendly area available in Pavilion 3.', '2026-06-01 14:00:00'),
    (4, 'MCG Emergency Assembly Area', 'Melbourne Cricket Ground, Yarra Park, VIC',     2000, 875, 'ACTIVE', 3, 'Major assembly point for eastern suburbs. Red Cross operating registration desk.', '2026-06-03 07:30:00'),
    (5, 'Dandenong Community Hub',     'Dandenong Civic Centre, Lonsdale St, VIC',       400,  60, 'ACTIVE', 3, 'Overflow site for residents displaced from structural red zones.', '2026-06-03 10:00:00');

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. resources
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS resources (
    id                INT          NOT NULL AUTO_INCREMENT,
    name              VARCHAR(150) NOT NULL,
    resourceType      VARCHAR(50)  NOT NULL,
    totalQuantity     INT          NOT NULL DEFAULT 0,
    availableQuantity INT          NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    assignedReportId  INT          NOT NULL DEFAULT 0,
    notes             TEXT         NULL,
    updatedAt         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO resources (id, name, resourceType, totalQuantity, availableQuantity, status, assignedReportId, notes) VALUES
    (1,  'Heavy Rescue Pumper Truck',        'VEHICLE',   3,  1, 'DEPLOYED',     2, 'Two units deployed to Western Sydney fire front. One unit on standby at Penrith depot.'),
    (2,  'Medical Response Van',             'VEHICLE',   5,  3, 'AVAILABLE',    0, 'Equipped with defibrillators, oxygen, and trauma kits. Available for immediate dispatch.'),
    (3,  'Emergency Helicopter Alpha',       'VEHICLE',   2,  1, 'DEPLOYED',     3, 'One helicopter conducting aerial search & rescue over Melbourne East. Second on standby.'),
    (4,  'Portable Water Pump Set',          'EQUIPMENT', 8,  3, 'DEPLOYED',     1, '5 units deployed to Brisbane CBD flood response. 3 units available at Rocklea warehouse.'),
    (5,  'Emergency Generator (50kVA)',      'EQUIPMENT', 6,  4, 'AVAILABLE',    0, 'Trailer-mounted. Can power a 200-person evacuation shelter for 72 hours on a full tank.'),
    (6,  'Inflatable Rescue Boat',           'EQUIPMENT', 4,  1, 'DEPLOYED',     1, '3 boats in active flood rescue ops in Brisbane. 1 unit available.'),
    (7,  'Search & Rescue Team Alpha',       'PERSONNEL', 12, 4, 'DEPLOYED',     3, '8 personnel conducting structural search in Melbourne East collapse zone.'),
    (8,  'Medical First Responder Team',     'PERSONNEL', 20, 8, 'DEPLOYED',     1, '12 personnel supporting Brisbane flood triage. 8 available for redeployment.'),
    (9,  'Emergency First Aid Kit (large)',  'MEDICAL',   50, 30, 'AVAILABLE',   0, 'Each kit treats up to 20 casualties. Stored at central depot. Requires cold chain for medications.'),
    (10, 'Trauma & Surgical Supply Pack',    'MEDICAL',   15,  9, 'DEPLOYED',    3, '6 packs deployed to Melbourne MCG triage station.'),
    (11, 'Emergency Food Ration (1-week)',   'SUPPLIES',  200, 120, 'AVAILABLE',  0, 'Sealed MRE packs. Suitable for 200 people for 1 week. Stored at Rocklea warehouse.'),
    (12, 'Portable Water Purification Unit', 'SUPPLIES',  10,   6, 'DEPLOYED',   1, '4 units providing clean water at Brisbane evacuation shelters.');

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. department_updates
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS department_updates (
    id             INT          NOT NULL AUTO_INCREMENT,
    reportId       INT          NOT NULL,
    departmentId   INT          NOT NULL,
    updatedById    INT          NOT NULL,
    updateText     TEXT         NOT NULL,
    responseStatus VARCHAR(20)  NOT NULL DEFAULT 'RESPONDING',
    updatedAt      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_du_report     FOREIGN KEY (reportId)     REFERENCES disaster_reports(id),
    CONSTRAINT fk_du_department FOREIGN KEY (departmentId) REFERENCES departments(id),
    CONSTRAINT fk_du_user       FOREIGN KEY (updatedById)  REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO department_updates (id, reportId, departmentId, updatedById, updateText, responseStatus, updatedAt) VALUES
    (1, 1, 1, 5, 'Fire & Rescue has deployed 5 pumper trucks and 2 swift water rescue teams to Brisbane CBD. Water levels are stabilising in George Street but Ann Street remains critical.',                          'RESPONDING',    '2026-06-01 12:00:00'),
    (2, 1, 2, 6, 'Medical Services has established a field triage station at King George Square. 3 ambulance crews on rotation. 14 casualties treated so far, 2 transported to Royal Brisbane Hospital.',             'RESPONDING',    '2026-06-01 13:30:00'),
    (3, 1, 1, 5, 'Pumping operations progressing well. George Street now passable to emergency vehicles. Estimated full road clearance in 18 hours. Requesting additional sandbag delivery to Ann Street sector.',   'RESPONDING',    '2026-06-01 18:00:00'),
    (4, 2, 1, 5, 'Firebreak established along Richmond Road. Fire front has been slowed but wind change expected at 1800hrs. All residents within 2km radius advised to evacuate immediately.',                       'RESPONDING',    '2026-06-02 17:00:00'),
    (5, 3, 2, 6, 'Mass casualty event confirmed. Triage centre operational at MCG. 47 serious injuries, 12 critical. Requesting additional trauma surgeons and blood supply from metropolitan hospitals.',            'NEEDS_SUPPORT', '2026-06-03 09:00:00'),
    (6, 5, 1, 5, 'Highway now cleared. Both townships have road access restored. Final debris removal expected to complete by end of day. Recommend downgrade of status to resolved.',                                'COMPLETED',     '2026-05-29 16:00:00');
