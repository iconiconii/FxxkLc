import { test, expect } from '@playwright/test';

test.describe('基础冒烟测试', () => {
  test('应用程序启动检查', async ({ page }) => {
    // Navigate to home page
    await page.goto('/');
    
    // Wait for page to load
    await page.waitForLoadState('networkidle');
    
    // Check that page is not showing error
    const hasError = await page.isVisible('text=404') || 
                     await page.isVisible('text=Error') ||
                     await page.isVisible('text=错误') ||
                     await page.isVisible('text=Internal Server Error');
    
    expect(hasError).toBe(false);
    
    // Check that page has some content (not blank)
    const body = await page.textContent('body');
    expect(body?.length).toBeGreaterThan(10);
  });

  test('登录页面可访问', async ({ page }) => {
    // Navigate to login page
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    
    // Check for login form elements
    const hasEmailField = await page.isVisible('input[type=email]') ||
                          await page.isVisible('[name=email]') ||
                          await page.isVisible('[data-testid=email]');
                          
    const hasPasswordField = await page.isVisible('input[type=password]') ||
                             await page.isVisible('[name=password]') ||
                             await page.isVisible('[data-testid=password]');
                             
    const hasLoginButton = await page.isVisible('button[type=submit]') ||
                           await page.isVisible('button:has-text("登录")') ||
                           await page.isVisible('[data-testid=login-button]');
    
    expect(hasEmailField).toBe(true);
    expect(hasPasswordField).toBe(true);  
    expect(hasLoginButton).toBe(true);
  });

  test('基础页面响应', async ({ page }) => {
    const pages = ['/', '/login'];
    
    for (const pagePath of pages) {
      const response = await page.goto(pagePath);
      expect(response?.status()).toBeLessThan(500);
      
      await page.waitForLoadState('networkidle');
      
      // Verify page loaded (has some content)
      const title = await page.title();
      expect(title.length).toBeGreaterThan(0);
    }
  });
});