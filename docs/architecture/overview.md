# Architecture overview

The system is a modular monorepo:

1. **React client** - role-aware navigation and dashboards. It must treat
   visibility as a UX feature only; all authorization is enforced by the API.
2. **Spring Boot API** - stateless REST endpoints, BCrypt password hashing,
   JWT access tokens, method-level RBAC, JPA repositories, Flyway migrations,
   audit fields, validation, and security event logging.
3. **PostgreSQL** - source of truth for users, departments, semesters, courses,
   enrollments, marks, attendance, risk flags, interventions, and reports.

## Planned API modules

- `/api/auth` - registration, login, rotating refresh tokens, logout revocation,
  and one-time password reset delivery through the configured mail provider
- `/api/users` - admin account management and profiles
- `/api/departments` and `/api/semesters` - academic structure
- `/api/courses` and `/api/enrollments` - HOD/professor/student workflows
- `/api/attendance` and `/api/marks` - professor data entry
- `/api/risk` - course-level risk calculation and account locks
- `/api/interventions` - HOD assignment, advisor outcomes, HOD unlocking
- `/api/reports` - role-specific tables and downloadable exports
- `/api/transcripts` - student transcript generation/download

## Implemented API slice

- `POST /api/auth/register` - creates a student account only; passwords are
  BCrypt-hashed before persistence.
- `POST /api/auth/login` - returns a short-lived signed JWT containing the
  user ID, email, and role.
- `GET /api/courses` - public course catalogue.
- `POST /api/courses/{courseId}/enroll` - student-only enrollment in the active
  semester. The service rejects duplicate enrollments and the seventh course.
- `PUT /api/risk/enrollments/{enrollmentId}` - professor-only academic record
  update. Course marks are calculated as Assignment 20%, CAT 30%, and Final
  Exam 50%; attendance below 70% or a calculated mark below 50% flags the
  enrollment and locks the student account.
- `POST /api/interventions` - advisor-only intervention creation for a locked
  student.
- `PUT /api/interventions/{interventionId}/resolve` - advisor-only outcome
  submission.
- `PUT /api/interventions/students/{studentId}/unlock` - HOD/Admin-only
  account unlocking after support has been coordinated.
- `POST /api/interventions/assign` - HOD-only advisor assignment for a
  department student.
- `GET /api/admin/advisors` - department-scoped advisor list for HOD users.
- `GET /api/risk/at-risk` - HOD/Admin/Advisor queue of flagged enrollments.
- `GET /api/courses/mine` - student's current enrollment list.
- `GET /api/interventions/mine` - advisor's assigned intervention queue.
- `GET /api/interventions/student` - student's intervention history.
- `GET /api/transcripts` - student's transcript rows.
- `GET /api/transcripts/download` - student's transcript as CSV.
- `GET /api/reports/at-risk.csv` - downloadable at-risk report for
  Admin/HOD/Advisor users.
- `GET /api/reports/overview` - role-scoped chart data for department risk,
  risk causes, semester trend, and intervention outcomes.
- `GET /api/risk/professor/enrollments` - professor's assigned course
  enrollments for attendance and mark entry.
- `POST /api/attendance/sessions` - professor creates one weekly class session
  for an assigned course.
- `PUT /api/attendance/sessions/{sessionId}/students/{studentId}` - professor
  records or corrects a student's presence for that session.

The React client now renders API-backed tables for users, departments, courses,
at-risk enrollments, transcripts, and intervention queues. Loading, empty, and
permission/error states are shown explicitly rather than displaying sample
records.

The Admin workspace provides staff-account and semester forms. The HOD
workspace provides course creation and professor-assignment forms; role
authorization remains enforced by the API.

The JWT is accepted only from the `Authorization: Bearer <token>` header.
Invalid or expired tokens do not establish an authenticated security context.

Production deployment should terminate HTTPS at the reverse proxy, expose only
the React application and API, restrict Actuator to health/info, and supply all
credentials, signing keys, mail credentials, and TLS certificates through
managed secret storage. Refresh-token rotation/revocation and password-reset
delivery are implemented by the API and frontend.
