BEGIN;

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

CREATE INDEX IF NOT EXISTS idx_intervention_case_activities_case
    ON intervention_case_activities (case_id, created_at ASC, activity_id ASC);

COMMIT;
