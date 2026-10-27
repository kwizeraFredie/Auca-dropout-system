import { useEffect, useState } from 'react';
import client from '../api/client';
import DataTable from '../components/DataTable';

const departments = [
  ['NETWORK_AND_COMMUNICATION_SYSTEMS', 'Network and Communication Systems'],
  ['INFORMATION_MANAGEMENT', 'Information Management'],
  ['SOFTWARE_ENGINEERING', 'Software Engineering'],
];

export default function AdminPage({ title }) {
  const [message, setMessage] = useState('');
  const [rows, setRows] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [departmentRows, setDepartmentRows] = useState([]);
  const [courses, setCourses] = useState([]);
  const [professors, setProfessors] = useState([]);
  const [form, setForm] = useState({});

  async function load() {
    if (title === 'Users') {
      const { data } = await client.get('/admin/users');
      setRows(data);
    }
    if (title === 'Departments') {
      const [{ data: departmentRows }, { data: semesterRows }] = await Promise.all([
        client.get('/admin/departments'), client.get('/admin/semesters'),
      ]);
      setRows(departmentRows);
      setDepartmentRows(departmentRows);
      setSemesters(semesterRows);
    }
    if (title === 'Courses') {
      const [{ data: courseRows }, { data: userRows }, { data: departmentsList }] = await Promise.all([
        client.get('/courses'), client.get('/admin/professors'), client.get('/admin/departments'),
      ]);
      setCourses(courseRows);
      setProfessors(userRows.filter((user) => user.role === 'PROFESSOR'));
      setDepartmentRows(departmentsList);
    }
  }
  useEffect(() => { load().catch(() => setMessage('Records could not be loaded.')); }, [title]);

  function update(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submitStaff(event) {
    event.preventDefault();
    try {
      await client.post('/admin/users', {
        ...form, role: form.role, department: form.department || null,
      });
      setMessage('Staff account created.');
      setForm({});
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Staff account could not be created.'); }
  }

  async function submitSemester(event) {
    event.preventDefault();
    try {
      await client.post('/admin/semesters', {
        ...form, active: Boolean(form.active),
      });
      setMessage('Three-month semester created.');
      setForm({});
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Semester could not be created.'); }
  }

  async function submitCourse(event) {
    event.preventDefault();
    try {
      await client.post('/admin/courses', {
        code: form.code, title: form.title, credits: Number(form.credits), departmentId: Number(form.departmentId),
      });
      setMessage('Course created.');
      setForm({});
    } catch (error) { setMessage(error.response?.data?.error || 'Course could not be created.'); }
  }

  async function assignProfessor(event) {
    event.preventDefault();
    try {
      await client.put(`/admin/courses/${form.courseId}/professor/${form.professorId}`);
      setMessage('Professor assigned to course.');
      setForm({});
    } catch (error) { setMessage(error.response?.data?.error || 'Professor could not be assigned.'); }
  }

  return <div className="content">
    <div className="table-page-header"><div><p className="eyebrow">AUCA ADMINISTRATION</p><h2>{title}</h2><p>{title === 'Users' ? 'Create HOD, Professor, and Advisor accounts.' : title === 'Departments' ? 'Review departments and manage three-month semesters.' : 'Create department courses and assign professors.'}</p></div><button className="text-button" onClick={() => load()}>Refresh</button></div>
    {message && <p className={message.includes('could not') ? 'form-error' : 'action-success'}>{message}</p>}
    {title === 'Users' && <><FormCard title="Create staff account" onSubmit={submitStaff}><TextField label="First name" value={form.firstName} onChange={(value) => update('firstName', value)} required /><TextField label="Last name" value={form.lastName} onChange={(value) => update('lastName', value)} required /><TextField label="Email" type="email" value={form.email} onChange={(value) => update('email', value)} required /><TextField label="Temporary password" type="password" value={form.temporaryPassword} onChange={(value) => update('temporaryPassword', value)} required /><SelectField label="Role" value={form.role} onChange={(value) => update('role', value)} options={['HOD', 'PROFESSOR', 'ADVISOR']} required /><SelectField label="Department" value={form.department} onChange={(value) => update('department', value)} options={departments.map(([value, label]) => ({ value, label }))} required /></FormCard><DataTable columns={[{ key: 'firstName', label: 'Name', render: (row) => `${row.firstName} ${row.lastName}` }, { key: 'email', label: 'Email' }, { key: 'role', label: 'Role' }, { key: 'department', label: 'Department' }]} rows={rows} emptyMessage="No users found." /></>}
    {title === 'Departments' && <><FormCard title="Create semester" onSubmit={submitSemester}><TextField label="Name" value={form.name} onChange={(value) => update('name', value)} placeholder="Semester II" required /><TextField label="Academic year" value={form.academicYear} onChange={(value) => update('academicYear', value)} placeholder="2026/2027" required /><TextField label="Starts on" type="date" value={form.startsOn} onChange={(value) => update('startsOn', value)} required /><TextField label="Ends on" type="date" value={form.endsOn} onChange={(value) => update('endsOn', value)} required /><label className="check-field"><input type="checkbox" checked={Boolean(form.active)} onChange={(event) => update('active', event.target.checked)} /> Active semester</label></FormCard><DataTable columns={[{ key: 'code', label: 'Code' }, { key: 'name', label: 'Department' }]} rows={rows} emptyMessage="No departments found." /><h3 className="section-title">Semesters</h3><DataTable columns={[{ key: 'name', label: 'Name' }, { key: 'academicYear', label: 'Academic year' }, { key: 'startsOn', label: 'Starts' }, { key: 'endsOn', label: 'Ends' }, { key: 'active', label: 'Status', render: (row) => row.active ? <span className="status-pill success">ACTIVE</span> : <span className="status-pill open">INACTIVE</span> }]} rows={semesters} emptyMessage="No semesters found." /></>}
    {title === 'Courses' && <><FormCard title="Create course" onSubmit={submitCourse}><TextField label="Course code" value={form.code} onChange={(value) => update('code', value)} required /><TextField label="Course title" value={form.title} onChange={(value) => update('title', value)} required /><TextField label="Credits" type="number" value={form.credits} onChange={(value) => update('credits', value)} required /><SelectField label="Department" value={form.departmentId} onChange={(value) => update('departmentId', value)} options={departmentRows.map((department) => ({ value: department.id, label: department.name }))} required /></FormCard><DataTable columns={[{ key: 'code', label: 'Code' }, { key: 'title', label: 'Course' }, { key: 'departmentId', label: 'Department' }, { key: 'professorId', label: 'Professor' }]} rows={courses} emptyMessage="No courses found." /><FormCard title="Assign professor" onSubmit={assignProfessor}><SelectField label="Course" value={form.courseId} onChange={(value) => update('courseId', value)} options={courses.map((course) => ({ value: course.id, label: `${course.code} — ${course.title}` }))} required /><SelectField label="Professor" value={form.professorId} onChange={(value) => update('professorId', value)} options={professors.map((user) => ({ value: user.id, label: `${user.firstName} ${user.lastName} (${user.email})` }))} required /></FormCard></>}
  </div>;
}

function FormCard({ title, onSubmit, children }) {
  return <section className="panel form-card"><h3>{title}</h3><form onSubmit={onSubmit} className="admin-form">{children}<button className="primary-button compact">Save</button></form></section>;
}

function TextField({ label, value = '', onChange, type = 'text', ...props }) {
  return <label>{label}<input type={type} value={value} onChange={(event) => onChange(event.target.value)} {...props} /></label>;
}

function SelectField({ label, value = '', onChange, options, ...props }) {
  return <label>{label}<select value={value} onChange={(event) => onChange(event.target.value)} {...props}><option value="">Select</option>{options.map((option) => { const item = typeof option === 'string' ? { value: option, label: option } : option; return <option key={item.value} value={item.value}>{item.label}</option>; })}</select></label>;
}
