import { Page } from '@playwright/test';
import { testUser } from './test-data';

export class AuthHelper {
  constructor(private page: Page) {}

  async login() {
    // Navigate to login page
    await this.page.goto('/login');
    
    // Wait for login form to be visible
    await this.page.waitForSelector('[data-testid=email], input[type=email], [name=email]');
    
    // Fill login form - try different selectors
    const emailSelector = await this.getEmailSelector();
    const passwordSelector = await this.getPasswordSelector();
    const loginButtonSelector = await this.getLoginButtonSelector();
    
    await this.page.fill(emailSelector, testUser.email);
    await this.page.fill(passwordSelector, testUser.password);
    
    // Click login button
    await this.page.click(loginButtonSelector);
    
    // Wait for successful login - check for dashboard or redirect
    try {
      await this.page.waitForURL('/dashboard', { timeout: 10000 });
    } catch {
      // If dashboard doesn't exist, wait for successful navigation away from login
      await this.page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
    }
    
    // Wait for page to be fully loaded
    await this.page.waitForLoadState('networkidle');
  }

  async logout() {
    // Try to find and click logout button
    const logoutSelectors = [
      '[data-testid=logout-button]',
      '[data-testid=user-menu] [role=menuitem]:has-text("登出")',
      '[data-testid=user-menu] [role=menuitem]:has-text("退出")',
      'button:has-text("登出")',
      'button:has-text("退出")',
      'a[href="/logout"]'
    ];

    for (const selector of logoutSelectors) {
      try {
        if (await this.page.isVisible(selector)) {
          await this.page.click(selector);
          break;
        }
      } catch {
        continue;
      }
    }
    
    // Wait for logout to complete
    await this.page.waitForURL('/login', { timeout: 5000 });
  }

  private async getEmailSelector(): Promise<string> {
    const selectors = [
      '[data-testid=email]',
      'input[type=email]',
      '[name=email]',
      '#email',
      'input[placeholder*="邮箱"]',
      'input[placeholder*="email"]'
    ];

    for (const selector of selectors) {
      if (await this.page.isVisible(selector)) {
        return selector;
      }
    }
    return selectors[0]; // fallback
  }

  private async getPasswordSelector(): Promise<string> {
    const selectors = [
      '[data-testid=password]',
      'input[type=password]',
      '[name=password]',
      '#password',
      'input[placeholder*="密码"]',
      'input[placeholder*="password"]'
    ];

    for (const selector of selectors) {
      if (await this.page.isVisible(selector)) {
        return selector;
      }
    }
    return selectors[0]; // fallback
  }

  private async getLoginButtonSelector(): Promise<string> {
    const selectors = [
      '[data-testid=login-button]',
      'button[type=submit]',
      'button:has-text("登录")',
      'button:has-text("登入")',
      'button:has-text("Login")',
      'input[type=submit]'
    ];

    for (const selector of selectors) {
      if (await this.page.isVisible(selector)) {
        return selector;
      }
    }
    return selectors[0]; // fallback
  }

  async ensureLoggedIn() {
    // Check if already logged in by looking for user-specific elements
    const loggedInIndicators = [
      '[data-testid=user-menu]',
      '[data-testid=logout-button]',
      'nav:has-text("Dashboard")',
      'a[href="/dashboard"]'
    ];

    let isLoggedIn = false;
    for (const indicator of loggedInIndicators) {
      if (await this.page.isVisible(indicator)) {
        isLoggedIn = true;
        break;
      }
    }

    if (!isLoggedIn) {
      await this.login();
    }
  }
}