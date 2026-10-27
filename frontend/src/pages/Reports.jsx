import { ShieldAlert } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import client from '../api/client';
import DataTable from '../components/DataTable';

export default function Reports() {
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    client.get('/reports/overview').then(({ data }) => setReport(data))
      .catch(() => setError('Reports could not be loaded. Check your connection or permissions.'));
  }, []);
  if (error) return <section className="empty-state"><h2>Reports unavailable</h2><p>{error}</p></section>;
  if (!report) return <section className="empty-state"><h2>Loading reports…</h2></section>;
  const colors = ['#23769a', '#e8ad35', '#7c5aa6'];
  async function downloadReport() {
    const response = await client.get('/reports/at-risk.csv', { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'auca-at-risk-report.csv';
    link.click();
    URL.revokeObjectURL(url);
  }
  const riskColumns = [
    { key: 'enrollmentId', label: 'Enrollment' },
    { key: 'studentId', label: 'Student ID' },
    { key: 'courseId', label: 'Course' },
    { key: 'reason', label: 'Reason' },
    { key: 'status', label: 'Status', render: () => <span className="status-pill risk">AT RISK</span> },
  ];
  const riskRows = report.recentRiskFlags.map((risk) => ({ ...risk, status: 'AT RISK' }));
  return <div className="content">
    <div className="welcome"><div><p className="eyebrow">ANALYTICS</p><h2>Student success reports</h2><p>Live academic risk and intervention indicators from the AUCA database.</p></div><button className="download-button" onClick={downloadReport}>Download CSV</button></div>
    <div className="metrics"><article className="metric warning"><div className="metric-icon"><ShieldAlert size={20} /></div><p>Total active risk flags</p><strong>{report.totalAtRisk}</strong><span>Across all recorded enrollments</span></article></div>
    <div className="report-grid">
      <ChartPanel title="At-risk students by department"><ResponsiveContainer width="100%" height={260}><PieChart><Pie data={report.byDepartment} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label>{report.byDepartment.map((entry, index) => <Cell key={entry.name} fill={colors[index % colors.length]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></ChartPanel>
      <ChartPanel title="Risk causes"><ResponsiveContainer width="100%" height={260}><BarChart data={report.riskCauses}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" tick={{ fontSize: 10 }} /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" fill="#e8ad35" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></ChartPanel>
      <ChartPanel title="Risk trend by semester"><ResponsiveContainer width="100%" height={260}><LineChart data={report.trend}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Line type="monotone" dataKey="value" stroke="#23769a" strokeWidth={3} /></LineChart></ResponsiveContainer></ChartPanel>
      <ChartPanel title="Intervention outcomes"><ResponsiveContainer width="100%" height={260}><BarChart data={report.interventionStatuses}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" fill="#7c5aa6" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></ChartPanel>
    </div>
    <section className="panel report-table-panel">
      <div className="panel-heading"><div><p className="eyebrow">DETAIL TABLE</p><h3>Recent at-risk enrollments</h3></div><span className="legend">Showing latest {riskRows.length} flags</span></div>
      <DataTable columns={riskColumns} rows={riskRows} emptyMessage="No students are currently flagged at risk." />
    </section>
  </div>;
}

function ChartPanel({ title, children }) {
  return <section className="panel report-panel"><div className="panel-heading"><h3>{title}</h3></div>{children}</section>;
}
