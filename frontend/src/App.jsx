import { BarChart3, BookOpen, GraduationCap, LayoutDashboard, LogOut, ShieldAlert, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useAuth } from './auth/AuthContext';
import Login from './pages/Login';
import TablePage from './pages/TablePage';
import client from './api/client';
import Reports from './pages/Reports';
import AdminPage from './pages/AdminPage';
import RiskPage from './pages/RiskPage';

const roleViews = {
  ADMIN: ['Overview', 'Users', 'Departments', 'Reports'],
  HOD: ['Overview', 'Courses', 'At-risk students', 'Reports'],
  PROFESSOR: ['Overview', 'My courses', 'Attendance & marks'],
  ADVISOR: ['Overview', 'Assigned students', 'Interventions', 'Reports'],
  STUDENT: ['Overview', 'Available courses', 'My transcript', 'My interventions'],
};

export default function App() {
  const { user, signOut } = useAuth();
  const [active, setActive] = useState('Overview');
  const [role, setRole] = useState('HOD');
  useEffect(() => {
    if (user) setRole(user.role);
  }, [user]);
  if (!user) return <Login />;

  const views = roleViews[role];
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><GraduationCap size={24} /></div>
          <div><strong>AUCA</strong><span>Student Success</span></div>
        </div>
        <div className="campus-label">GISHUSHU CAMPUS</div>
        <nav>
          {views.map((view, index) => (
            <button className={active === view ? 'nav-item active' : 'nav-item'} onClick={() => setActive(view)} key={view}>
              {index === 0 ? <LayoutDashboard size={18} /> : index === 1 ? <BookOpen size={18} /> : index === 2 ? <Users size={18} /> : <BarChart3 size={18} />}
              {view}
            </button>
          ))}
        </nav>
        <button className="logout" onClick={signOut}><LogOut size={18} /> Sign out</button>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <div><p className="eyebrow">AUCA GISHUSHU CAMPUS</p><h1>{active}</h1></div>
          <span className="role-switcher">Role: {role}</span>
        </header>
        {active === 'Overview' ? <Overview onOpenRisk={role === 'HOD' ? () => setActive('At-risk students') : undefined} /> : active === 'Reports' ? <Reports /> : active === 'At-risk students' ? <RiskPage /> : ((role === 'ADMIN' && ['Users', 'Departments'].includes(active)) || (role === 'HOD' && active === 'Courses')) ? <AdminPage title={active} /> : <TablePage title={active} />}
      </main>
    </div>
  );
}

function Overview({ onOpenRisk }) {
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    client.get('/reports/overview').then(({ data }) => setReport(data))
      .catch(() => setError('Dashboard data could not be loaded. Check your connection or permissions.'));
  }, []);
  if (error) return <section className="empty-state"><h2>Dashboard unavailable</h2><p>{error}</p></section>;
  if (!report) return <section className="empty-state"><h2>Loading dashboard…</h2></section>;
  return <div className="content">
    <div className="welcome"><div><p className="eyebrow">GOOD MORNING</p><h2>Student success starts with early action.</h2><p>Monitor academic signals, coordinate support, and help every AUCA student progress.</p></div><div className="date-chip">Updated today<br /><strong>26 Oct 2026</strong></div></div>
    <div className="metrics">
      <Metric label="Students monitored" value={report.totalStudents} detail="Registered accounts" icon={Users} />
      <Metric label="Students at risk" value={report.totalAtRisk} detail="Needs follow-up" icon={ShieldAlert} warning />
      <Metric label="Active courses" value={report.activeCourses} detail="Course catalogue" icon={BookOpen} />
      <Metric label="Interventions resolved" value={`${report.resolvedInterventionPercent}%`} detail="Recorded outcomes" icon={BarChart3} />
    </div>
    <div className="dashboard-grid">
      <section className="panel"><div className="panel-heading"><div><p className="eyebrow">RISK MONITORING</p><h3>Risk by semester</h3></div><span className="legend">Database records</span></div><div className="chart"><div className="chart-bars">{report.trend.map((point) => <div className="bar-group" key={point.name}><div className="bar" style={{ height: `${Math.max(point.value * 10, 4)}%` }} /><small>{point.name.replace('Semester ', 'S')}</small></div>)}</div></div></section>
      <section className="panel"><div className="panel-heading"><div><p className="eyebrow">ACTION REQUIRED</p><h3>Recent risk flags</h3></div>{onOpenRisk && <button className="text-button" onClick={onOpenRisk}>View all</button>}</div>{report.recentRiskFlags.map((risk) => <div className="risk-row" key={risk.enrollmentId}><div className="avatar">ID</div><div><strong>Student {risk.studentId}</strong><span>Enrollment {risk.enrollmentId} · Course {risk.courseId}</span></div><em>{risk.reason}</em></div>)}</section>
    </div>
  </div>;
}

function Metric({ label, value, detail, icon: Icon, warning }) {
  return <article className={warning ? 'metric warning' : 'metric'}><div className="metric-icon"><Icon size={20} /></div><p>{label}</p><strong>{value}</strong><span>{detail}</span></article>;
}
