import { GraduationCap } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { confirmPasswordReset, requestPasswordReset } from '../api/client';

export default function Login() {
  const { signIn, signUp, loading } = useAuth();
  const resetToken = new URLSearchParams(window.location.search).get('token');
  const [registering, setRegistering] = useState(false);
  const [resetting, setResetting] = useState(Boolean(resetToken));
  const [resetPassword, setResetPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [studentNumber, setStudentNumber] = useState('');
  const [department, setDepartment] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      if (resetting) {
        if (resetToken) {
          await confirmPasswordReset(resetToken, resetPassword);
          window.history.replaceState({}, '', window.location.pathname);
          setError('Password reset completed. You can now sign in.');
          setResetting(false);
        } else {
          await requestPasswordReset(email);
          setError('If the account exists, a password reset email has been sent.');
        }
      } else if (registering) {
        await signUp({ email, password, firstName, lastName, studentNumber, department });
      } else {
        await signIn({ email, password });
      }
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'Unable to complete the request.');
    }
  }

  return <main className="login-page">
    <section className="login-card">
      <div className="brand login-brand"><div className="brand-mark"><GraduationCap size={24} /></div><div><strong>AUCA</strong><span>Student Success</span></div></div>
      <p className="eyebrow">GISHUSHU CAMPUS</p>
      <h1>{resetting ? 'Reset your password' : registering ? 'Create your student account' : 'Welcome back'}</h1>
      <p className="login-copy">{resetToken ? 'Choose a new password for your AUCA account.' : resetting ? 'Enter your email and we will send a secure reset link.' : registering ? 'Register to view courses, enroll, and follow your academic progress.' : 'Sign in to monitor academic progress and coordinate student support.'}</p>
      <form onSubmit={submit}>
        {registering && <><label>First name<input required value={firstName} onChange={(event) => setFirstName(event.target.value)} /></label><label>Last name<input required value={lastName} onChange={(event) => setLastName(event.target.value)} /></label><label>Student number<input value={studentNumber} onChange={(event) => setStudentNumber(event.target.value)} /></label><label>Department<select required value={department} onChange={(event) => setDepartment(event.target.value)}><option value="">Select department</option><option value="NETWORK_AND_COMMUNICATION_SYSTEMS">Network and Communication Systems</option><option value="INFORMATION_MANAGEMENT">Information Management</option><option value="SOFTWARE_ENGINEERING">Software Engineering</option></select></label></>}
        {(!resetting || !resetToken) && <label>Email address<input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>}
        {(!resetting || resetToken) && <label>Password<input type="password" minLength={12} required value={resetToken ? resetPassword : password} onChange={(event) => resetToken ? setResetPassword(event.target.value) : setPassword(event.target.value)} /></label>}
        {error && <p className="form-error">{error}</p>}
        <button className="primary-button" disabled={loading}>{loading ? 'Please wait…' : resetToken ? 'Set new password' : resetting ? 'Send reset link' : registering ? 'Create account' : 'Sign in'}</button>
      </form>
      {!registering && !resetting && <button className="link-button" onClick={() => { setResetting(true); setError(''); }}>Forgot password?</button>}
      <button className="link-button" onClick={() => { setResetting(false); setRegistering(!registering); setError(''); }}>{registering || resetting ? 'Back to sign in' : 'New student? Create an account'}</button>
    </section>
  </main>;
}
