import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

let refreshRequest = null;
let csrfRequest = null;

async function ensureCsrfToken() {
  if (document.cookie.split('; ').some((value) => value.startsWith('XSRF-TOKEN='))) return;
  csrfRequest ??= client.get('/auth/csrf').finally(() => { csrfRequest = null; });
  await csrfRequest;
}

client.interceptors.request.use((config) => {
  const csrfCookie = document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='));
  if (csrfCookie) config.headers['X-XSRF-TOKEN'] = decodeURIComponent(csrfCookie.split('=')[1]);
  return config;
});

client.interceptors.response.use(undefined, async (error) => {
  const original = error.config;
  if (error.response?.status !== 401 || !original || original._retry
      || original.url?.includes('/auth/')) return Promise.reject(error);

  original._retry = true;
  refreshRequest ??= ensureCsrfToken().then(() => client.post('/auth/refresh'))
    .then(({ data }) => data)
    .finally(() => { refreshRequest = null; });
  try {
    await refreshRequest;
    return client(original);
  } catch (refreshError) {
    logout();
    return Promise.reject(refreshError);
  }
});

export default client;

export async function registerStudent(payload) {
  await ensureCsrfToken();
  const { data } = await client.post('/auth/register', payload);
  return data;
}

export async function login(payload) {
  await ensureCsrfToken();
  const { data } = await client.post('/auth/login', payload);
  return data;
}

export async function logout() {
  await ensureCsrfToken();
  client.post('/auth/logout').catch(() => {});
}

export async function requestPasswordReset(email) {
  await ensureCsrfToken();
  return client.post('/auth/password-reset/request', { email });
}

export async function confirmPasswordReset(token, newPassword) {
  await ensureCsrfToken();
  return client.post('/auth/password-reset/confirm', { token, newPassword });
}
