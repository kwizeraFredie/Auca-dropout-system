import { useEffect, useMemo, useState } from 'react';
import client from '../api/client';
import DataTable from '../components/DataTable';

const configs = {
  'At-risk students': {
    endpoint: '/risk/at-risk',
    columns: [
      { key: 'id', label: 'Enrollment' }, { key: 'studentId', label: 'Student ID' },
      { key: 'courseId', label: 'Course' }, { key: 'attendancePercent', label: 'Attendance' },
      { key: 'riskFlagged', label: 'Status', render: () => <span className="status-pill risk">AT RISK</span> },
    ],
  },
  'My transcript': {
    endpoint: '/transcripts',
    columns: [
      { key: 'semesterId', label: 'Semester' }, { key: 'courseCode', label: 'Code' },
      { key: 'courseTitle', label: 'Course' }, { key: 'attendancePercent', label: 'Attendance' },
      { key: 'courseMark', label: 'Mark' },
      { key: 'status', label: 'Status', render: (row) => <span className={row.status === 'PASS' ? 'status-pill success' : 'status-pill risk'}>{row.status}</span> },
    ],
  },
  'Assigned students': {
    endpoint: '/interventions/mine',
    columns: [
      { key: 'id', label: 'Intervention' }, { key: 'studentId', label: 'Student ID' },
      { key: 'notes', label: 'Notes' },
      { key: 'status', label: 'Status', render: (row) => <span className={row.status === 'RESOLVED' ? 'status-pill success' : 'status-pill open'}>{row.status}</span> },
      { key: 'createdAt', label: 'Created' },
    ],
  },
  Interventions: {
    endpoint: '/interventions/mine',
    columns: [
      { key: 'id', label: 'Intervention' }, { key: 'studentId', label: 'Student ID' },
      { key: 'notes', label: 'Notes' },
      { key: 'status', label: 'Status', render: (row) => <span className={row.status === 'RESOLVED' ? 'status-pill success' : 'status-pill open'}>{row.status}</span> },
      { key: 'outcome', label: 'Outcome' },
    ],
  },
  'My interventions': {
    endpoint: '/interventions/student',
    columns: [
      { key: 'id', label: 'Intervention' }, { key: 'advisorId', label: 'Advisor' },
      { key: 'notes', label: 'Notes' }, { key: 'outcome', label: 'Outcome' },
      { key: 'status', label: 'Status', render: (row) => <span className={row.status === 'RESOLVED' ? 'status-pill success' : 'status-pill open'}>{row.status}</span> },
    ],
  },
  Users: {
    endpoint: '/admin/users',
    columns: [
      { key: 'firstName', label: 'Name', render: (row) => `${row.firstName} ${row.lastName}` },
      { key: 'email', label: 'Email' }, { key: 'role', label: 'Role' }, { key: 'department', label: 'Department' },
      { key: 'accountLocked', label: 'Account', render: (row) => row.accountLocked ? <span className="status-pill risk">LOCKED</span> : <span className="status-pill success">ACTIVE</span> },
    ],
  },
  Courses: { endpoint: '/courses', columns: courseColumns() },
  Departments: { endpoint: '/admin/departments', columns: [{ key: 'code', label: 'Code' }, { key: 'name', label: 'Department' }] },
  'Available courses': { endpoint: '/courses', columns: courseColumns() },
  'My courses': { endpoint: '/courses', columns: courseColumns() },
};

function courseColumns() {
  return [
    { key: 'code', label: 'Code' }, { key: 'title', label: 'Course' },
    { key: 'credits', label: 'Credits' }, { key: 'departmentId', label: 'Department' },
    { key: 'professorId', label: 'Professor' },
  ];
}

export default function TablePage({ title }) {
  if (title === 'Attendance & marks') return <AttendancePage />;
  if (title === 'Assigned students' || title === 'Interventions') return <InterventionPage title={title} />;

  const config = configs[title] || configs.Courses;
  const [rows, setRows] = useState([]);
  const [state, setState] = useState('loading');
  const [actionMessage, setActionMessage] = useState('');

  useEffect(() => {
    let active = true;
    setState('loading');
    client.get(config.endpoint).then(({ data }) => {
      if (active) { setRows(Array.isArray(data) ? data : []); setState('ready'); }
    }).catch(() => { if (active) setState('error'); });
    return () => { active = false; };
  }, [config.endpoint]);

  async function enroll(courseId) {
    setActionMessage('');
    try {
      await client.post(`/courses/${courseId}/enroll`);
      setActionMessage('Course enrollment completed.');
    } catch (error) {
      setActionMessage(error.response?.data?.error || 'Enrollment could not be completed.');
    }
  }

  async function downloadTranscript() {
    const response = await client.get('/transcripts/download', { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'auca-transcript.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  const description = useMemo(() => ({
    Users: 'Manage AUCA staff and student accounts.',
    Courses: 'Courses available across the Information Technology faculty.',
    'At-risk students': 'Students requiring academic follow-up.',
    'My transcript': 'Your academic record and current risk status.',
  }[title] || 'Live records from the AUCA academic system.'), [title]);

  return <div className="content">
    <div className="table-page-header"><div><p className="eyebrow">AUCA ACADEMIC RECORDS</p><h2>{title}</h2><p>{description}</p></div><span className="table-actions"><button className="text-button" onClick={() => window.location.reload()}>Refresh</button>{title === 'My transcript' && <button className="download-button" onClick={() => downloadTranscript().catch(() => setActionMessage('Transcript download could not be completed.'))}>Download CSV</button>}</span></div>
    {state === 'loading' && <div className="table-empty">Loading records…</div>}
    {state === 'error' && <div className="table-empty error-state">Records could not be loaded. Check your connection or permissions.</div>}
    {actionMessage && <p className={actionMessage.includes('completed') ? 'action-success' : 'form-error'}>{actionMessage}</p>}
    {state === 'ready' && title === 'Available courses'
      ? <div className="course-cards">{rows.map((course) => <article className="course-card" key={course.id}><div><span className="course-code">{course.code}</span><h3>{course.title}</h3><p>{course.credits} credits · Department {course.departmentId}</p></div><button className="primary-button compact" onClick={() => enroll(course.id)}>Enroll</button></article>)}</div>
      : state === 'ready' && <DataTable columns={config.columns} rows={rows} emptyMessage="No records are available yet." />}
  </div>;
}

function AttendancePage() {
  const [rows, setRows] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [courses, setCourses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [semesterId, setSemesterId] = useState('');
  const [sessionDate, setSessionDate] = useState('');
  const [selectedSession, setSelectedSession] = useState('');
  const [message, setMessage] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [marks, setMarks] = useState({});

  async function load() {
    const [enrollmentResponse, sessionResponse, courseResponse, semesterResponse] = await Promise.all([
      client.get('/risk/professor/enrollments'), client.get('/attendance/sessions/mine'),
      client.get('/courses'), client.get('/admin/semesters'),
    ]);
    setRows(enrollmentResponse.data);
    setSessions(sessionResponse.data);
    setCourses(courseResponse.data);
    setSemesters(semesterResponse.data);
  }
  useEffect(() => { load().catch(() => setMessage('Attendance records could not be loaded.')); }, []);

  async function createSession(event) {
    event.preventDefault();
    try {
      await client.post('/attendance/sessions', { courseId: Number(courseId), semesterId: Number(semesterId), sessionDate });
      setMessage('Weekly class session created.');
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'The weekly session could not be created.'); }
  }

  async function mark(studentId, present) {
    try {
      await client.put(`/attendance/sessions/${selectedSession}/students/${studentId}`, { present });
      setMessage(`Attendance marked ${present ? 'present' : 'absent'}.`);
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Attendance could not be recorded.'); }
  }

  function startEditing(row) {
    setEditingId(row.id);
    setMarks({ assignmentMark: row.assignmentMark ?? '', catMark: row.catMark ?? '', finalExamMark: row.finalExamMark ?? '' });
    setMessage('');
  }

  async function saveMarks() {
    try {
      await client.put(`/risk/enrollments/${editingId}`, Object.fromEntries(Object.entries(marks).map(([key, value]) => [key, value === '' ? null : Number(value)])));
      setEditingId(null);
      setMessage('Academic record saved and risk status recalculated.');
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Academic record could not be saved.'); }
  }

  const input = (row, field) => editingId === row.id
    ? <input className="small-input" type="number" min="0" max="100" value={marks[field]} onChange={(event) => setMarks({ ...marks, [field]: event.target.value })} />
    : row[field] ?? '—';

  return <div className="content">
    <div className="table-page-header"><div><p className="eyebrow">PROFESSOR WORKSPACE</p><h2>Attendance & marks</h2><p>Create one weekly session per course and record each enrolled student.</p></div><button className="text-button" onClick={() => load()}>Refresh</button></div>
    <section className="panel attendance-form"><h3>Create weekly class session</h3><form onSubmit={createSession} className="inline-form"><label>Course<select required value={courseId} onChange={(event) => setCourseId(event.target.value)}><option value="">Select course</option>{courses.map((course) => <option value={course.id} key={course.id}>{course.code} — {course.title}</option>)}</select></label><label>Semester<select required value={semesterId} onChange={(event) => setSemesterId(event.target.value)}><option value="">Select semester</option>{semesters.map((semester) => <option value={semester.id} key={semester.id}>{semester.name} — {semester.academicYear}</option>)}</select></label><label>Class date<input type="date" required value={sessionDate} onChange={(event) => setSessionDate(event.target.value)} /></label><button className="primary-button compact">Create session</button></form></section>
    {message && <p className={message.includes('could not') ? 'form-error' : 'action-success'}>{message}</p>}
    <section className="panel attendance-form"><label>Attendance session<select value={selectedSession} onChange={(event) => setSelectedSession(event.target.value)}><option value="">Select a weekly session</option>{sessions.map((session) => <option value={session.id} key={session.id}>Course {session.courseId} · {session.sessionDate}</option>)}</select></label></section>
    <DataTable columns={[
      { key: 'id', label: 'Enrollment' }, { key: 'studentId', label: 'Student ID' }, { key: 'courseId', label: 'Course' },
      { key: 'attendancePercent', label: 'Attendance' },
      { key: 'assignmentMark', label: 'Assignment', render: (row) => input(row, 'assignmentMark') },
      { key: 'catMark', label: 'CAT', render: (row) => input(row, 'catMark') },
      { key: 'finalExamMark', label: 'Final exam', render: (row) => input(row, 'finalExamMark') },
      { key: 'courseMark', label: 'Course mark' },
      { key: 'riskFlagged', label: 'Risk', render: (row) => row.riskFlagged ? <span className="status-pill risk">AT RISK</span> : <span className="status-pill success">ON TRACK</span> },
      { key: 'actions', label: 'Actions', render: (row) => editingId === row.id ? <span className="attendance-actions"><button onClick={saveMarks}>Save</button><button onClick={() => setEditingId(null)}>Cancel</button></span> : <span className="attendance-actions"><button onClick={() => startEditing(row)}>Edit marks</button><button disabled={!selectedSession} onClick={() => mark(row.studentId, true)}>Present</button><button disabled={!selectedSession} onClick={() => mark(row.studentId, false)}>Absent</button></span> },
    ]} rows={rows} emptyMessage="No students are enrolled in your assigned courses." />
  </div>;
}

function InterventionPage({ title }) {
  const [rows, setRows] = useState([]);
  const [state, setState] = useState('loading');
  const [outcomes, setOutcomes] = useState({});
  const [message, setMessage] = useState('');

  async function load() {
    setState('loading');
    try {
      const { data } = await client.get('/interventions/mine');
      setRows(data);
      setState('ready');
    } catch { setState('error'); }
  }
  useEffect(() => { load(); }, []);

  async function resolve(id) {
    try {
      await client.put(`/interventions/${id}/resolve`, { outcome: outcomes[id] || '' });
      setMessage('Intervention resolved.');
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Intervention could not be resolved.'); }
  }

  return <div className="content">
    <div className="table-page-header"><div><p className="eyebrow">ADVISOR WORKSPACE</p><h2>{title}</h2><p>Review assigned students and record intervention outcomes.</p></div><button className="text-button" onClick={load}>Refresh</button></div>
    {message && <p className={message.includes('could not') ? 'form-error' : 'action-success'}>{message}</p>}
    {state === 'loading' && <div className="table-empty">Loading interventions…</div>}
    {state === 'error' && <div className="table-empty error-state">Interventions could not be loaded.</div>}
    {state === 'ready' && <DataTable columns={[
      { key: 'id', label: 'Intervention' }, { key: 'studentId', label: 'Student ID' }, { key: 'notes', label: 'Notes' },
      { key: 'status', label: 'Status', render: (row) => <span className={row.status === 'RESOLVED' ? 'status-pill success' : 'status-pill open'}>{row.status}</span> },
      { key: 'outcome', label: 'Outcome', render: (row) => row.status === 'OPEN' ? <input className="outcome-input" value={outcomes[row.id] ?? ''} onChange={(event) => setOutcomes({ ...outcomes, [row.id]: event.target.value })} placeholder="Add outcome" /> : row.outcome ?? '—' },
      { key: 'action', label: 'Action', render: (row) => row.status === 'OPEN' && <button className="table-action" disabled={!outcomes[row.id]?.trim()} onClick={() => resolve(row.id)}>Resolve</button> },
    ]} rows={rows} emptyMessage="No interventions are assigned yet." />}
  </div>;
}
