import { test, expect } from '@playwright/test';

test('password reset request uses a non-enumerating response', async ({ page }) => {
  await page.route('**/api/auth/password-reset/request', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      message: 'If the account exists, a password reset email has been sent',
    }) });
  });

  await page.goto('/');
  await page.getByRole('button', { name: 'Forgot password?' }).click();
  await page.getByLabel('Email address').fill('student@example.com');
  await page.getByRole('button', { name: 'Send reset link' }).click();

  await expect(page.getByText('If the account exists, a password reset email has been sent')).toBeVisible();
});

test('password reset link submits a new password and clears the token URL', async ({ page }) => {
  await page.route('**/api/auth/password-reset/confirm', async (route) => {
    const request = route.request();
    expect((await request.postDataJSON()).token).toBe('one-time-token');
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      message: 'Password reset completed',
    }) });
  });

  await page.goto('/?token=one-time-token');
  await page.getByLabel('Password').fill('new-password-123');
  await page.getByRole('button', { name: 'Set new password' }).click();

  await expect(page).toHaveURL('/');
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
});
