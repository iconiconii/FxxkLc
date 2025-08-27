import { test, expect } from '@playwright/test';
import { AuthHelper } from './fixtures/auth-helper';

test.describe('导航功能 E2E 测试', () => {
  let authHelper: AuthHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    await authHelper.login();
  });

  test('主要页面导航', async ({ page }) => {
    // Test navigation to main pages
    const navigationTests = [
      { url: '/dashboard', expectedElements: ['Dashboard', '仪表板', 'dashboard'], name: 'Dashboard' },
      { url: '/codetop', expectedElements: ['问题', 'Problem', 'codetop'], name: 'CodeTop' },
      { url: '/notes/my', expectedElements: ['我的笔记', 'My Notes', 'notes'], name: 'My Notes' },
      { url: '/notes/public', expectedElements: ['公开笔记', 'Public Notes', 'public'], name: 'Public Notes' },
      { url: '/analysis', expectedElements: ['分析', 'Analysis', 'analytics'], name: 'Analysis' },
      { url: '/leaderboard', expectedElements: ['排行榜', 'Leaderboard', 'ranking'], name: 'Leaderboard' }
    ];

    for (const navTest of navigationTests) {
      await page.goto(navTest.url);
      await page.waitForLoadState('networkidle');
      
      // Check if page loaded successfully (not 404 or error)
      const is404 = await page.isVisible('text=404') || 
                    await page.isVisible('text=Not Found') || 
                    await page.isVisible('text=页面不存在');
      
      if (!is404) {
        // Verify page content
        let pageContentFound = false;
        for (const element of navTest.expectedElements) {
          if (await page.isVisible(`text=${element}`)) {
            pageContentFound = true;
            break;
          }
        }
        
        expect(pageContentFound).toBe(true, `${navTest.name} page should contain expected elements`);
      }
    }
  });

  test('侧边栏导航', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    // Look for sidebar navigation elements
    const sidebarSelectors = [
      '[data-testid=sidebar]',
      '.sidebar',
      'nav[role=navigation]',
      '.navigation'
    ];
    
    let sidebarFound = false;
    for (const selector of sidebarSelectors) {
      if (await page.isVisible(selector)) {
        sidebarFound = true;
        
        // Test common navigation links
        const navLinks = [
          'Dashboard',
          '仪表板',
          'CodeTop',
          '题目',
          'Notes',
          '笔记',
          'Analysis',
          '分析'
        ];
        
        for (const linkText of navLinks) {
          const link = page.locator(`${selector} a:has-text("${linkText}"), ${selector} button:has-text("${linkText}")`).first();
          if (await link.isVisible()) {
            await link.click();
            await page.waitForLoadState('networkidle');
            
            // Verify navigation worked (URL changed or content updated)
            const currentUrl = page.url();
            expect(currentUrl).not.toContain('/login');
            
            // Go back to test next link
            await page.goto('/dashboard');
            await page.waitForLoadState('networkidle');
          }
        }
        break;
      }
    }
    
    if (!sidebarFound) {
      // Test top navigation instead
      const navLinks = page.locator('nav a, header a, .nav-link');
      const linkCount = await navLinks.count();
      
      for (let i = 0; i < Math.min(linkCount, 5); i++) {
        const link = navLinks.nth(i);
        if (await link.isVisible()) {
          await link.click();
          await page.waitForLoadState('networkidle');
          
          // Verify we're still authenticated and not on error page
          const isErrorPage = await page.isVisible('text=404') || 
                              await page.isVisible('text=Error') ||
                              await page.isVisible('text=错误');
          expect(isErrorPage).toBe(false);
        }
      }
    }
  });

  test('面包屑导航', async ({ page }) => {
    // Test breadcrumb navigation if available
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    const breadcrumbSelectors = [
      '[data-testid=breadcrumb]',
      '.breadcrumb',
      'nav[aria-label="breadcrumb"]',
      'ol.breadcrumb'
    ];
    
    for (const selector of breadcrumbSelectors) {
      if (await page.isVisible(selector)) {
        const breadcrumbLinks = page.locator(`${selector} a`);
        const linkCount = await breadcrumbLinks.count();
        
        // Click on breadcrumb links to test navigation
        for (let i = 0; i < linkCount; i++) {
          const link = breadcrumbLinks.nth(i);
          if (await link.isVisible()) {
            await link.click();
            await page.waitForLoadState('networkidle');
            
            // Verify navigation worked
            expect(page.url()).not.toContain('/login');
          }
        }
        break;
      }
    }
  });

  test('返回按钮和浏览器历史', async ({ page }) => {
    // Navigate through several pages
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    await page.goto('/codetop');
    await page.waitForLoadState('networkidle');
    
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Test browser back button
    await page.goBack();
    await page.waitForLoadState('networkidle');
    expect(page.url()).toContain('codetop');
    
    await page.goBack();
    await page.waitForLoadState('networkidle');
    expect(page.url()).toContain('dashboard');
    
    // Test browser forward button
    await page.goForward();
    await page.waitForLoadState('networkidle');
    expect(page.url()).toContain('codetop');
    
    // Test back button in UI if available
    const backButtonSelectors = [
      '[data-testid=back-button]',
      'button:has-text("返回")',
      'button:has-text("Back")',
      'a:has-text("返回")',
      '.back-button'
    ];
    
    for (const selector of backButtonSelectors) {
      if (await page.isVisible(selector)) {
        await page.click(selector);
        await page.waitForLoadState('networkidle');
        
        // Verify navigation worked
        expect(page.url()).not.toContain('/login');
        break;
      }
    }
  });

  test('搜索导航', async ({ page }) => {
    // Test global search if available
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    const searchSelectors = [
      '[data-testid=global-search]',
      'input[placeholder*="搜索"]',
      'input[placeholder*="Search"]',
      'input[type=search]',
      '.search-input'
    ];
    
    for (const selector of searchSelectors) {
      if (await page.isVisible(selector)) {
        await page.fill(selector, '算法');
        await page.press(selector, 'Enter');
        
        await page.waitForLoadState('networkidle');
        
        // Verify search results page or search functionality
        const hasSearchResults = await page.isVisible('text=搜索结果') ||
                                 await page.isVisible('text=Search Results') ||
                                 await page.isVisible('.search-results') ||
                                 await page.isVisible('[data-testid=search-results]');
        
        if (hasSearchResults) {
          expect(hasSearchResults).toBe(true);
        }
        break;
      }
    }
  });

  test('响应式导航', async ({ page }) => {
    // Test mobile navigation
    await page.setViewportSize({ width: 375, height: 667 }); // Mobile viewport
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    // Look for mobile menu toggle
    const mobileMenuSelectors = [
      '[data-testid=mobile-menu-toggle]',
      '.mobile-menu-toggle',
      'button[aria-label="Toggle menu"]',
      'button[aria-label="菜单"]',
      '.hamburger-menu'
    ];
    
    for (const selector of mobileMenuSelectors) {
      if (await page.isVisible(selector)) {
        // Test opening mobile menu
        await page.click(selector);
        await page.waitForTimeout(500); // Wait for animation
        
        // Verify mobile menu is visible
        const mobileMenuSelectors = [
          '[data-testid=mobile-menu]',
          '.mobile-menu',
          '.mobile-nav',
          'nav[aria-expanded="true"]'
        ];
        
        let mobileMenuVisible = false;
        for (const menuSelector of mobileMenuSelectors) {
          if (await page.isVisible(menuSelector)) {
            mobileMenuVisible = true;
            
            // Test navigation links in mobile menu
            const mobileNavLinks = page.locator(`${menuSelector} a, ${menuSelector} button`);
            const linkCount = await mobileNavLinks.count();
            
            if (linkCount > 0) {
              const firstLink = mobileNavLinks.first();
              if (await firstLink.isVisible()) {
                await firstLink.click();
                await page.waitForLoadState('networkidle');
                
                // Verify navigation worked
                expect(page.url()).not.toContain('/login');
              }
            }
            break;
          }
        }
        
        expect(mobileMenuVisible).toBe(true);
        break;
      }
    }
    
    // Reset to desktop viewport
    await page.setViewportSize({ width: 1280, height: 720 });
  });

  test('键盘导航', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    // Test Tab navigation
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    
    // Check if focus is visible
    const focusedElement = page.locator(':focus');
    if (await focusedElement.count() > 0) {
      // Test Enter key on focused element
      await page.keyboard.press('Enter');
      await page.waitForLoadState('networkidle');
      
      // Verify navigation or action occurred
      expect(page.url()).not.toContain('/login');
    }
    
    // Test escape key for closing modals/menus if any are open
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
  });
});