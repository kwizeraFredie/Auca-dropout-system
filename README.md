# AUCA Student Dropout Management System

Monorepo foundation for Adventist University of Central Africa (AUCA), Gishushu
campus. The system monitors student attendance and academic performance, flags
course-level dropout risk, coordinates advisor interventions, and provides
role-specific dashboards and reports.

## Repository layout

- `backend/` - Spring Boot REST API and domain services
- `frontend/` - React/Vite web application
- `database/` - PostgreSQL migrations and seed data
- `infra/` - local and production infrastructure configuration
- `docs/` - architecture and business rules

## Core AUCA rules

- A student is at risk in an individual course when attendance is below **70%**
  or the course mark is below **50%**.
- A student may enroll in at most **six courses per semester**.
- Risk flags lock the student account until an Admin or the student's HOD
  unlocks it after an advisor intervention.
- HODs lead one of the three Information Technology departments:
  Network and Communication Systems, Information Management, or Software
  Engineering.
- Admins create HOD, Professor, and Advisor accounts. Students self-register.
- HODs create courses, assign professors, assign advisors to at-risk students,
  and review intervention outcomes.

## Local development

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Set the database URL, database
credentials, JWT secret, and allowed frontend origin before starting it.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The web client starts on `http://localhost:5173`.

The backend includes student registration/login and the
course-catalogue/enrollment workflow. Browser authentication uses HttpOnly
cookies; production deployments must use HTTPS and keep `SECURE_COOKIES=true`.
Set `SECURE_COOKIES=false` only for local HTTP development.

Professor academic-record updates now calculate course marks using Assignment
20%, CAT 30%, and Final Exam 50%. Attendance below 70% or a course mark below
50% flags the enrollment and locks the student's account. Advisors can record
interventions for locked students, and HOD/Admin users can unlock accounts.

Students can view their enrollment and transcript through the API, including a
CSV transcript download. Advisors can view their assigned intervention queue,
and Admin/HOD/Advisor users can download the at-risk CSV report.

The Reports view now renders live charts from `/api/reports/overview`:

- Department risk distribution — pie chart
- Risk causes — bar chart
- Risk trend by semester — line chart
- Intervention outcomes — bar chart

The same Reports view includes a detailed at-risk enrollments table beneath
the charts, populated from the same database-generated response.

Attendance is recorded per scheduled course session. Semesters must be exactly
three calendar months, and the database prevents more than one session for the
same course in the same week. A student taking six courses therefore has six
separate weekly attendance records. Enrollment attendance percentages are
recalculated from present/recorded sessions and feed the 70% risk rule.

The CSV report download uses the authenticated API client rather than an
unauthorized browser link.

Role navigation now includes live tables for administrative users, courses,
at-risk enrollments, student transcripts, and advisor interventions.

HOD users can select a department advisor, assign that advisor to an at-risk
student with intervention notes, and unlock department student accounts after
support. Student registration records the student's department so these
authorization boundaries can be enforced.

Department-scoped staff accounts (HOD, Professor, and Advisor) must always
declare one of the three AUCA departments. HOD course management, professor
assignment, advisor assignment, and student unlocking are checked against that
department on the backend.

Reports and at-risk lists are scoped by the authenticated role: Admin sees all
records, HOD sees their department, Professor sees their assigned courses,
Advisor sees assigned students, and Student sees only their own records.
Accounts can only be unlocked after a resolved intervention exists.

The API also sends restrictive CSP, frame, and referrer-policy headers. CSRF is
disabled because this API uses stateless bearer tokens; production deployments
must still terminate HTTPS and provide a managed JWT secret.

Admin users can create HOD, Professor, and Advisor accounts and configure
three-month semesters. HOD users can create department courses and assign
professors from the web workspace.

Advisors can resolve assigned interventions directly from the Interventions
table by entering an outcome. The backend verifies that the authenticated
advisor owns the intervention before marking it resolved.

### PostgreSQL

```bash
docker compose -f infra/docker-compose.yml up -d
```

Never commit production credentials or JWT secrets. Use environment variables or
a managed secrets store.

Refresh tokens are rotated and revoked server-side, and password reset uses
one-time tokens delivered through the configured managed SMTP provider. Set the
mail and reset-link environment variables before enabling those flows.
