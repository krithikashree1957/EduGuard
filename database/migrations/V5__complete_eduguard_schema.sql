BEGIN;

-- This migration is safe to rerun. It preserves all existing rows and adds
-- the tables required by the current JPA entity model.

CREATE TABLE IF NOT EXISTS intervention_cases (
    case_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN
        ('ACADEMIC', 'ATTENDANCE', 'ENGAGEMENT', 'FINANCIAL', 'WELLBEING', 'OTHER')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN
        ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status VARCHAR(20) NOT NULL CHECK (status IN
        ('OPEN', 'IN_PROGRESS', 'ON_HOLD', 'CLOSED')),
    assigned_staff_id VARCHAR(100),
    follow_up_date DATE,
    resolution_summary VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intervention_case_activities (
    activity_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES intervention_cases(case_id),
    activity_type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    actor_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
        WHERE t.relname = 'intervention_case_activities'
          AND c.contype = 'f'
          AND a.attname = 'case_id'
    ) THEN
        ALTER TABLE intervention_case_activities
            ADD CONSTRAINT fk_intervention_case_activities_case
            FOREIGN KEY (case_id) REFERENCES intervention_cases(case_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_reference VARCHAR(150) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP')),
    event_type VARCHAR(80) NOT NULL,
    template_id VARCHAR(80) NOT NULL,
    subject VARCHAR(200),
    message VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    error_summary VARCHAR(500),
    idempotency_key VARCHAR(200),
    delivery_mode VARCHAR(20) NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT uq_notification_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS notification_attempts (
    attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications(notification_id),
    outcome VARCHAR(20) NOT NULL,
    error_summary VARCHAR(500),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_support_barriers (
    barrier_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    barrier_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    severity VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_support_barrier_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE TABLE IF NOT EXISTS student_financial_support (
    financial_support_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    support_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_financial_support_status
        CHECK (status IN ('REQUESTED', 'IN_REVIEW', 'APPROVED', 'DECLINED', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS student_wellbeing_requests (
    wellbeing_request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(student_id),
    support_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_wellbeing_request_status
        CHECK (status IN ('REQUESTED', 'IN_REVIEW', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_intervention_cases_student
    ON intervention_cases (student_id, updated_at DESC, case_id DESC);
CREATE INDEX IF NOT EXISTS idx_intervention_cases_status
    ON intervention_cases (status);
CREATE INDEX IF NOT EXISTS idx_intervention_cases_priority
    ON intervention_cases (priority);
CREATE INDEX IF NOT EXISTS idx_intervention_cases_assigned_staff
    ON intervention_cases (assigned_staff_id);
CREATE INDEX IF NOT EXISTS idx_intervention_cases_follow_up
    ON intervention_cases (follow_up_date, status);
CREATE INDEX IF NOT EXISTS idx_intervention_case_activities_case
    ON intervention_case_activities (case_id, created_at ASC, activity_id ASC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient
    ON notifications (recipient_reference, created_at DESC, notification_id DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_status
    ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notification_attempts_notification
    ON notification_attempts (notification_id, attempted_at ASC, attempt_id ASC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_student_support_barrier
    ON student_support_barriers (student_id, barrier_type)
    WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_student_support_barriers_student
    ON student_support_barriers (student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_student_financial_support_student
    ON student_financial_support (student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_student_wellbeing_requests_student
    ON student_wellbeing_requests (student_id, created_at DESC);

COMMIT;
