import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
  const { baseURL } = config.projects[0].use;
  
  // Start browser and create a new page
  const browser = await chromium.launch();
  const page = await browser.newPage();

  try {
    // Wait for the application to be ready
    console.log('Waiting for application to be ready...');
    await page.goto(baseURL || 'http://localhost:3000');
    
    // Wait for the page to load completely
    await page.waitForLoadState('networkidle');
    
    // Check if login page is accessible
    await page.goto((baseURL || 'http://localhost:3000') + '/login');
    await page.waitForSelector('[data-testid=email], input[type=email], [name=email]', { timeout: 10000 });
    
    console.log('Application is ready for testing');
    
    // Store authentication state if needed
    // This is where you could login with a test user and save the auth state
    // await page.fill('[data-testid=email]', 'test@example.com');
    // await page.fill('[data-testid=password]', 'password123');
    // await page.click('[data-testid=login-button]');
    // await page.waitForURL('/dashboard');
    // await page.context().storageState({ path: 'tests/e2e/fixtures/auth.json' });
    
  } catch (error) {
    console.error('Global setup failed:', error);
    throw error;
  } finally {
    await browser.close();
  }
}

export default globalSetup;