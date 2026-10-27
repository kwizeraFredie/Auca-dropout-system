import { useEffect, useState } from 'react';
import client from '../api/client';
import DataTable from '../components/DataTable';

export default function RiskPage() {
  const [rows, setRows] = useState([]);
  const [advisors, setAdvisors] = useState([]);
  const [advisorId, setAdvisorId] = useState('');
  const [notes, setNotes] = useState('');
  const [message, setMessage] = useState('');

  async function load() {
    const [{ data: riskRows }, { data: advisorRows }] = await Promise.all([
      client.get('/risk/at-risk'), client.get('/admin/advisors'),
    ]);
    setRows(riskRows);
    setAdvisors(advisorRows);
  }
  useEffect(() => { load().catch(() => setMessage('Risk records could not be loaded.')); }, []);

  async function assign(studentId) {
    if (!advisorId || !notes.trim()) {
      setMessage('Select an advisor and enter intervention notes.');
      return;
    }
    try {
      await client.post('/interventions/assign', { advisorId, studentId, notes });
      setMessage('Advisor assigned successfully.');
      setNotes('');
    } catch (error) { setMessage(error.response?.data?.error || 'Advisor assignment failed.'); }
  }

  async function unlock(studentId) {
    try {
      await client.put(`/interventions/students/${studentId}/unlock`);
      setMessage('Student account unlocked.');
      await load();
    } catch (error) { setMessage(error.response?.data?.error || 'Account could not be unlocked.'); }
  }

  return <div className="content">
    <div className="table-page-header"><div><p className="eyebrow">HOD WORKSPACE</p><h2>At-risk students</h2><p>Assign departmental advisors and unlock accounts after support is completed.</p></div><button className="text-button" onClick={() => load()}>Refresh</button></div>
    {message && <p className={message.includes('failed') || message.includes('could not') ? 'form-error' : 'action-success'}>{message}</p>}
    <section className="panel form-card"><h3>Intervention assignment</h3><div className="admin-form"><label>Advisor<select value={advisorId} onChange={(event) => setAdvisorId(event.target.value)}><option value="">Select advisor</option>{advisors.map((advisor) => <option key={advisor.id} value={advisor.id}>{advisor.firstName} {advisor.lastName} · {advisor.email}</option>)}</select></label><label>Notes<textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Describe the support needed" /></label></div></section>
    <DataTable columns={[
      { key: 'id', label: 'Enrollment' }, { key: 'studentId', label: 'Student ID' }, { key: 'courseId', label: 'Course' },
      { key: 'attendancePercent', label: 'Attendance' }, { key: 'riskFlagged', label: 'Status', render: () => <span className="status-pill risk">AT RISK</span> },
      { key: 'actions', label: 'Actions', render: (row) => <span className="attendance-actions"><button onClick={() => assign(row.studentId)}>Assign advisor</button><button onClick={() => unlock(row.studentId)}>Unlock</button></span> },
    ]} rows={rows} emptyMessage="No students are currently flagged at risk." />
  </div>;
}
