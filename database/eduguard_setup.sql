

BEGIN;

CREATE TABLE IF NOT EXISTS students (
    student_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_code VARCHAR(20) UNIQUE NOT NULL,
    class_name VARCHAR(30) NOT NULL,
    term VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance (
    attendance_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    total_classes INTEGER NOT NULL DEFAULT 0,
    attended_classes INTEGER NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_total_classes CHECK (total_classes >= 0),
    CONSTRAINT chk_attended_classes
        CHECK (attended_classes >= 0 AND attended_classes <= total_classes)
);

CREATE TABLE IF NOT EXISTS assessments (
    assessment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    assessment_name VARCHAR(100) NOT NULL,
    marks_obtained NUMERIC(5,2) NOT NULL,
    maximum_marks NUMERIC(5,2) NOT NULL,
    assessment_date DATE DEFAULT CURRENT_DATE,
    CONSTRAINT chk_marks_obtained CHECK (marks_obtained >= 0),
    CONSTRAINT chk_maximum_marks CHECK (maximum_marks > 0),
    CONSTRAINT chk_marks_range CHECK (marks_obtained <= maximum_marks)
);

CREATE TABLE IF NOT EXISTS engagement (
    engagement_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    participation_score NUMERIC(5,2) NOT NULL,
    assignment_completion_rate NUMERIC(5,2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_participation_score
        CHECK (participation_score BETWEEN 0 AND 100),
    CONSTRAINT chk_assignment_completion
        CHECK (assignment_completion_rate BETWEEN 0 AND 100)
);

-- Sample records are fictional and intended for local development/testing.
-- Explicit identity values are used so the related student_id values match.
INSERT INTO students
    (student_id, student_code, class_name, term, created_at)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'STU001', 'CSE-A', '2026-T1', CURRENT_TIMESTAMP),
    (2, 'STU002', 'CSE-A', '2026-T1', CURRENT_TIMESTAMP),
    (3, 'STU003', 'CSE-A', '2026-T1', CURRENT_TIMESTAMP),
    (4, 'STU004', 'CSE-B', '2026-T1', CURRENT_TIMESTAMP),
    (5, 'STU005', 'CSE-B', '2026-T1', CURRENT_TIMESTAMP),
    (6, 'STU006', 'CSE-A', '2026-T2', CURRENT_TIMESTAMP),
    (7, 'STU007', 'ECE-A', '2026-T1', CURRENT_TIMESTAMP),
    (8, 'STU008', 'ECE-A', '2026-T1', CURRENT_TIMESTAMP),
    (9, 'STU009', 'ECE-B', '2026-T1', CURRENT_TIMESTAMP),
    (10, 'STU010', 'ECE-B', '2026-T2', CURRENT_TIMESTAMP)
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO attendance
    (attendance_id, student_id, total_classes, attended_classes, recorded_at)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 40, 36, CURRENT_TIMESTAMP),
    (2, 2, 40, 24, CURRENT_TIMESTAMP),
    (3, 3, 40, 32, CURRENT_TIMESTAMP),
    (4, 4, 40, 38, CURRENT_TIMESTAMP),
    (5, 5, 40, 28, CURRENT_TIMESTAMP),
    (6, 6, 20, 18, CURRENT_TIMESTAMP),
    (7, 7, 40, 35, CURRENT_TIMESTAMP),
    (8, 8, 40, 22, CURRENT_TIMESTAMP),
    (9, 9, 40, 30, CURRENT_TIMESTAMP),
    (10, 10, 20, 16, CURRENT_TIMESTAMP)
ON CONFLICT (attendance_id) DO NOTHING;

INSERT INTO assessments
    (assessment_id, student_id, assessment_name, marks_obtained,
     maximum_marks, assessment_date)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 'Mathematics Test', 82.00, 100.00, DATE '2026-09-15'),
    (2, 2, 'Mathematics Test', 45.00, 100.00, DATE '2026-09-15'),
    (3, 3, 'Mathematics Test', 68.00, 100.00, DATE '2026-09-15'),
    (4, 4, 'Mathematics Test', 91.00, 100.00, DATE '2026-09-15'),
    (5, 5, 'Mathematics Test', 55.00, 100.00, DATE '2026-09-15'),
    (6, 6, 'Mathematics Test', 79.00, 100.00, DATE '2026-09-15'),
    (7, 7, 'Mathematics Test', 85.00, 100.00, DATE '2026-09-15'),
    (8, 8, 'Mathematics Test', 48.00, 100.00, DATE '2026-09-15'),
    (9, 9, 'Mathematics Test', 72.00, 100.00, DATE '2026-09-15'),
    (10, 10, 'Mathematics Test', 88.00, 100.00, DATE '2026-09-15')
ON CONFLICT (assessment_id) DO NOTHING;

INSERT INTO engagement
    (engagement_id, student_id, participation_score,
     assignment_completion_rate, recorded_at)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 85.00, 95.00, CURRENT_TIMESTAMP),
    (2, 2, 40.00, 55.00, CURRENT_TIMESTAMP),
    (3, 3, 65.00, 75.00, CURRENT_TIMESTAMP),
    (4, 4, 92.00, 98.00, CURRENT_TIMESTAMP),
    (5, 5, 50.00, 60.00, CURRENT_TIMESTAMP),
    (6, 6, 78.00, 90.00, CURRENT_TIMESTAMP),
    (7, 7, 88.00, 95.00, CURRENT_TIMESTAMP),
    (8, 8, 35.00, 50.00, CURRENT_TIMESTAMP),
    (9, 9, 70.00, 80.00, CURRENT_TIMESTAMP),
    (10, 10, 90.00, 100.00, CURRENT_TIMESTAMP)
ON CONFLICT (engagement_id) DO NOTHING;

-- Synchronize identity sequences after inserting explicit IDs.
SELECT setval(pg_get_serial_sequence('students', 'student_id'),
              COALESCE((SELECT MAX(student_id) FROM students), 1), true);
SELECT setval(pg_get_serial_sequence('attendance', 'attendance_id'),
              COALESCE((SELECT MAX(attendance_id) FROM attendance), 1), true);
SELECT setval(pg_get_serial_sequence('assessments', 'assessment_id'),
              COALESCE((SELECT MAX(assessment_id) FROM assessments), 1), true);
SELECT setval(pg_get_serial_sequence('engagement', 'engagement_id'),
              COALESCE((SELECT MAX(engagement_id) FROM engagement), 1), true);

COMMIT;
