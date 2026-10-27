CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL UNIQUE
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    student_number VARCHAR(40) UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    gender VARCHAR(30),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'HOD', 'PROFESSOR', 'ADVISOR', 'STUDENT')),
    department VARCHAR(80) REFERENCES departments(code),
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE semesters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (name, academic_year)
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    credits INTEGER NOT NULL CHECK (credits > 0),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    professor_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    semester_id BIGINT NOT NULL REFERENCES semesters(id),
    attendance_percent NUMERIC(5,2),
    assignment_mark NUMERIC(5,2),
    cat_mark NUMERIC(5,2),
    final_exam_mark NUMERIC(5,2),
    risk_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (student_id, course_id, semester_id)
);

CREATE TABLE interventions (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    advisor_id UUID NOT NULL REFERENCES users(id),
    notes TEXT NOT NULL,
    outcome TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

INSERT INTO departments (code, name) VALUES
    ('NETWORK_AND_COMMUNICATION_SYSTEMS', 'Network and Communication Systems'),
    ('INFORMATION_MANAGEMENT', 'Information Management'),
    ('SOFTWARE_ENGINEERING', 'Software Engineering');
