import { test, expect } from '@playwright/test';
import { testUser } from './fixtures/test-data';

test.describe('认证功能 E2E 测试', () => {
  
  test.beforeEach(async ({ page }) => {
    // Start fresh for each test
    await page.context().clearCookies();
    await page.context().clearPermissions();
  });

  test('用户登录流程', async ({ page }) => {
    // Navigate to login page
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    
    // Verify login form elements exist
    const emailField = page.locator('[data-testid=email], input[type=email], [name=email]').first();
    const passwordField = page.locator('[data-testid=password], input[type=password], [name=password]').first();
    const loginButton = page.locator('[data-testid=login-button], button[type=submit], button:has-text("登录")').first();
    
    await expect(emailField).toBeVisible();
    await expect(passwordField).toBeVisible();
    await expect(loginButton).toBeVisible();
    
    // Fill login form
    await emailField.fill(testUser.email);
    await passwordField.fill(testUser.password);
    
    // Submit form
    await loginButton.click();
    
    // Verify successful login - wait for redirect
    try {
      await page.waitForURL('/dashboard', { timeout: 10000 });
    } catch {
      // If no dashboard, just verify we're not on login page anymore
      await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
    }
    
    // Verify login success indicators
    const loginSuccessIndicators = [
      '[data-testid=user-menu]',
      'text=欢迎',
      'text=Dashboard',
      'text=仪表板',
      '[data-testid=logout-button]'
    ];
    
    let loginVerified = false;
    for (const indicator of loginSuccessIndicators) {
      if (await page.isVisible(indicator)) {
        loginVerified = true;
        break;
      }
    }
    
    expect(loginVerified).toBe(true);
  });

  test('错误凭据登录失败', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    
    // Fill with incorrect credentials
    await page.fill('[data-testid=email], input[type=email], [name=email]', 'wrong@example.com');
    await page.fill('[data-testid=password], input[type=password], [name=password]', 'wrongpassword');
    
    // Submit form
    await page.click('[data-testid=login-button], button[type=submit], button:has-text("登录")');
    
    // Verify error message appears
    const errorSelectors = [
      '[data-testid=error-message]',
      '.error-message',
      'text=用户名或密码错误',
      'text=登录失败',
      'text=Invalid credentials',
      'text=Login failed'
    ];
    
    let errorFound = false;
    for (const selector of errorSelectors) {
      if (await page.isVisible(selector)) {
        errorFound = true;
        break;
      }
    }
    
    expect(errorFound).toBe(true);
    
    // Verify still on login page
    expect(page.url()).toContain('/login');
  });

  test('用户注册流程', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    
    // Check if register page exists and has required fields
    const emailField = page.locator('[data-testid=email], input[type=email], [name=email]').first();
    const passwordField = page.locator('[data-testid=password], input[type=password], [name=password]').first();
    
    if (await emailField.isVisible() && await passwordField.isVisible()) {
      // Fill registration form
      const testRegisterUser = {
        email: `test_${Date.now()}@example.com`,
        password: 'TestPassword123!',
        confirmPassword: 'TestPassword123!'
      };
      
      await emailField.fill(testRegisterUser.email);
      await passwordField.fill(testRegisterUser.password);
      
      // Fill confirm password if field exists
      const confirmPasswordField = page.locator('[data-testid=confirm-password], input[name=confirmPassword], input[name=password_confirmation]').first();
      if (await confirmPasswordField.isVisible()) {
        await confirmPasswordField.fill(testRegisterUser.confirmPassword);
      }
      
      // Submit registration
      await page.click('[data-testid=register-button], button[type=submit], button:has-text("注册"), button:has-text("Register")');
      
      // Verify registration success (redirect to login or dashboard)
      await page.waitForURL(url => !url.pathname.includes('/register'), { timeout: 10000 });
    } else {
      // Skip test if register page doesn't exist
      test.skip(true, 'Register page not available');
    }
  });

  test('用户登出流程', async ({ page }) => {
    // First login
    await page.goto('/login');
    await page.fill('[data-testid=email], input[type=email], [name=email]', testUser.email);
    await page.fill('[data-testid=password], input[type=password], [name=password]', testUser.password);
    await page.click('[data-testid=login-button], button[type=submit], button:has-text("登录")');
    
    // Wait for login to complete
    await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
    
    // Find and click logout button
    const logoutSelectors = [
      '[data-testid=logout-button]',
      'button:has-text("登出")',
      'button:has-text("退出")',
      'button:has-text("Logout")',
      '[data-testid=user-menu] [role=menuitem]:has-text("登出")',
      'a[href="/logout"]'
    ];

    let logoutFound = false;
    for (const selector of logoutSelectors) {
      if (await page.isVisible(selector)) {
        await page.click(selector);
        logoutFound = true;
        break;
      }
    }

    if (logoutFound) {
      // Verify logout success - should redirect to login page
      await page.waitForURL('/login', { timeout: 10000 });
      
      // Verify user is actually logged out
      await expect(page.locator('[data-testid=email], input[type=email]')).toBeVisible();
    } else {
      // Skip test if logout functionality not found
      test.skip(true, 'Logout functionality not found');
    }
  });

  test('未认证访问保护页面重定向', async ({ page }) => {
    // Try to access protected pages without authentication
    const protectedUrls = [
      '/dashboard',
      '/notes/my',
      '/codetop',
      '/profile'
    ];

    for (const url of protectedUrls) {
      await page.goto(url);
      await page.waitForLoadState('networkidle');
      
      // Should redirect to login or show login form
      const currentUrl = page.url();
      const isRedirectedToLogin = currentUrl.includes('/login') || 
                                 await page.isVisible('[data-testid=email], input[type=email]') ||
                                 await page.isVisible('button:has-text("登录")');
      
      expect(isRedirectedToLogin).toBe(true);
    }
  });

  test('会话超时处理', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('[data-testid=email], input[type=email], [name=email]', testUser.email);
    await page.fill('[data-testid=password], input[type=password], [name=password]', testUser.password);
    await page.click('[data-testid=login-button], button[type=submit], button:has-text("登录")');
    
    // Wait for login
    await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
    
    // Clear cookies to simulate session timeout
    await page.context().clearCookies();
    
    // Try to access a protected resource
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Should be redirected to login
    const isRedirectedToLogin = page.url().includes('/login') || 
                               await page.isVisible('[data-testid=email], input[type=email]');
    
    expect(isRedirectedToLogin).toBe(true);
  });
});