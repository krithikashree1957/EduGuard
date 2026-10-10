BEGIN;

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

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_student_support_barrier
    ON student_support_barriers (student_id, barrier_type)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_student_support_barriers_student
    ON student_support_barriers (student_id, created_at DESC);

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

CREATE INDEX IF NOT EXISTS idx_student_financial_support_student
    ON student_financial_support (student_id, created_at DESC);

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

CREATE INDEX IF NOT EXISTS idx_student_wellbeing_requests_student
    ON student_wellbeing_requests (student_id, created_at DESC);

COMMIT;
