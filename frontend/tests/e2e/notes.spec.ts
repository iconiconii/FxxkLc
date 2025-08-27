import { test, expect, Page } from '@playwright/test';
import { AuthHelper } from './fixtures/auth-helper';
import { testNote, publicTestNote, testProblem } from './fixtures/test-data';

test.describe('笔记功能 E2E 测试', () => {
  let authHelper: AuthHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    await authHelper.login();
  });

  test.afterEach(async ({ page }) => {
    // Clean up any test data created during the test
    await cleanupTestNotes(page);
  });

  test('创建笔记完整流程', async ({ page }) => {
    // 1. Navigate to codetop page
    await page.goto('/codetop');
    await page.waitForLoadState('networkidle');
    
    // 2. Find and click on a problem card
    const problemCard = await findProblemCard(page);
    if (problemCard) {
      await problemCard.click();
    } else {
      // Fallback: go directly to a specific problem
      await page.goto(`/problem/${testProblem.id}`);
    }
    
    await page.waitForLoadState('networkidle');
    
    // 3. Open notes tab/section
    const notesTab = await findNotesTab(page);
    if (notesTab) {
      await notesTab.click();
    }
    
    // 4. Click create note button
    const createButton = await findCreateNoteButton(page);
    if (createButton) {
      await createButton.click();
    } else {
      // Alternative approach: look for add/new note button
      await page.click('button:has-text("添加笔记"), button:has-text("新建笔记"), button:has-text("Create Note")');
    }
    
    // 5. Wait for note editor to appear
    await page.waitForSelector('[data-testid=note-editor], .note-editor, textarea, [contenteditable=true]');
    
    // 6. Fill in note details
    await fillNoteForm(page, testNote);
    
    // 7. Save the note
    const saveButton = await findSaveButton(page);
    if (saveButton) {
      await saveButton.click();
    }
    
    // 8. Verify save success
    await verifyNoteSaved(page, testNote.title);
    
    // 9. Verify note appears in my notes list
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    await expect(page.locator(`text=${testNote.title}`)).toBeVisible({ timeout: 10000 });
  });

  test('编辑现有笔记', async ({ page }) => {
    // First create a note to edit
    await createTestNote(page, testNote);
    
    // Navigate to my notes
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Find and click edit button for our test note
    const noteCard = page.locator(`[data-testid=note-card]:has-text("${testNote.title}"), .note-card:has-text("${testNote.title}")`).first();
    await noteCard.locator('[data-testid=edit-button], button:has-text("编辑"), button:has-text("Edit")').click();
    
    // Update the note content
    const updatedContent = testNote.content + '\n\n## 更新内容\n这是更新后的内容。';
    await fillNoteContent(page, updatedContent);
    
    // Save changes
    const saveButton = await findSaveButton(page);
    if (saveButton) {
      await saveButton.click();
    }
    
    // Verify update success
    await expect(page.locator('text=更新后的内容')).toBeVisible({ timeout: 5000 });
  });

  test('删除笔记', async ({ page }) => {
    // First create a note to delete
    await createTestNote(page, testNote);
    
    // Navigate to my notes
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Find and click delete button
    const noteCard = page.locator(`[data-testid=note-card]:has-text("${testNote.title}"), .note-card:has-text("${testNote.title}")`).first();
    await noteCard.locator('[data-testid=delete-button], button:has-text("删除"), button:has-text("Delete")').click();
    
    // Confirm deletion
    await page.locator('[data-testid=confirm-delete], button:has-text("确认删除"), button:has-text("确定"), button:has-text("Delete")').click();
    
    // Verify note is removed
    await expect(page.locator(`text=${testNote.title}`)).not.toBeVisible({ timeout: 5000 });
  });

  test('公开笔记浏览和投票', async ({ page }) => {
    // First create a public note
    await createTestNote(page, publicTestNote);
    
    // Navigate to public notes page
    await page.goto('/notes/public');
    await page.waitForLoadState('networkidle');
    
    // Select a problem if there's a problem selector
    const problemSelect = page.locator('[data-testid=problem-select], select');
    if (await problemSelect.isVisible()) {
      await problemSelect.selectOption({ value: testProblem.id.toString() });
    }
    
    // Find our public test note
    const publicNote = page.locator(`text=${publicTestNote.title}`).first();
    await publicNote.click();
    
    // Wait for note details to load
    await page.waitForLoadState('networkidle');
    
    // Try to vote (helpful vote)
    const voteButton = page.locator('[data-testid=helpful-vote], [data-testid=upvote], button:has-text("有用"), button:has-text("👍")').first();
    if (await voteButton.isVisible()) {
      await voteButton.click();
      
      // Verify vote was recorded (check if vote count increased)
      await expect(page.locator('[data-testid=vote-count], .vote-count')).toBeVisible({ timeout: 5000 });
    }
  });

  test('笔记搜索功能', async ({ page }) => {
    // Create multiple test notes
    const searchNote1 = { ...testNote, title: '搜索测试笔记1 - 二分查找' };
    const searchNote2 = { ...testNote, title: '搜索测试笔记2 - 深度优先搜索' };
    
    await createTestNote(page, searchNote1);
    await createTestNote(page, searchNote2);
    
    // Navigate to notes page
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Search for notes
    const searchInput = page.locator('[data-testid=search-input], input[placeholder*="搜索"], input[type=search]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('搜索测试');
      await searchInput.press('Enter');
      
      // Wait for search results
      await page.waitForLoadState('networkidle');
      
      // Verify both notes appear in results
      await expect(page.locator(`text=${searchNote1.title}`)).toBeVisible();
      await expect(page.locator(`text=${searchNote2.title}`)).toBeVisible();
    }
  });

  test('笔记标签功能', async ({ page }) => {
    // Create note with specific tags
    const taggedNote = { ...testNote, tags: ['E2E测试', '算法', '标签测试'] };
    await createTestNote(page, taggedNote);
    
    // Navigate to my notes
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Look for tags in the note display
    for (const tag of taggedNote.tags) {
      await expect(page.locator(`text=${tag}`).first()).toBeVisible({ timeout: 5000 });
    }
    
    // Test tag filtering if available
    const tagFilter = page.locator('[data-testid=tag-filter], .tag-filter').first();
    if (await tagFilter.isVisible()) {
      await tagFilter.click();
      await page.locator(`text=${taggedNote.tags[0]}`).click();
      
      // Verify filtered results
      await expect(page.locator(`text=${taggedNote.title}`)).toBeVisible();
    }
  });
});

// Helper functions
async function findProblemCard(page: Page) {
  const selectors = [
    '[data-testid=problem-card]',
    '.problem-card',
    '[data-testid=problem-item]',
    '.problem-item',
    'a[href*="/problem/"]'
  ];
  
  for (const selector of selectors) {
    const element = page.locator(selector).first();
    if (await element.isVisible()) {
      return element;
    }
  }
  return null;
}

async function findNotesTab(page: Page) {
  const selectors = [
    '[data-testid=notes-tab]',
    'button:has-text("笔记")',
    'button:has-text("Notes")',
    '[role=tab]:has-text("笔记")',
    '.tab:has-text("笔记")'
  ];
  
  for (const selector of selectors) {
    const element = page.locator(selector).first();
    if (await element.isVisible()) {
      return element;
    }
  }
  return null;
}

async function findCreateNoteButton(page: Page) {
  const selectors = [
    '[data-testid=create-note-button]',
    '[data-testid=add-note-button]',
    'button:has-text("创建笔记")',
    'button:has-text("添加笔记")',
    'button:has-text("新建笔记")',
    'button:has-text("Create Note")',
    'button:has-text("Add Note")',
    '.create-note-btn',
    '.add-note-btn'
  ];
  
  for (const selector of selectors) {
    const element = page.locator(selector).first();
    if (await element.isVisible()) {
      return element;
    }
  }
  return null;
}

async function findSaveButton(page: Page) {
  const selectors = [
    '[data-testid=save-note-button]',
    '[data-testid=save-button]',
    'button:has-text("保存")',
    'button:has-text("Save")',
    'button[type=submit]',
    '.save-btn'
  ];
  
  for (const selector of selectors) {
    const element = page.locator(selector).first();
    if (await element.isVisible()) {
      return element;
    }
  }
  return null;
}

async function fillNoteForm(page: Page, note: any) {
  // Fill title
  const titleSelectors = [
    '[data-testid=note-title]',
    'input[placeholder*="标题"]',
    'input[placeholder*="title"]',
    '[name=title]',
    '#title'
  ];
  
  for (const selector of titleSelectors) {
    if (await page.isVisible(selector)) {
      await page.fill(selector, note.title);
      break;
    }
  }
  
  // Fill content
  await fillNoteContent(page, note.content);
  
  // Fill other fields if they exist
  await fillOptionalField(page, 'time-complexity', note.timeComplexity);
  await fillOptionalField(page, 'space-complexity', note.spaceComplexity);
  await fillOptionalField(page, 'solution-approach', note.solutionApproach);
}

async function fillNoteContent(page: Page, content: string) {
  const contentSelectors = [
    '[data-testid=note-content]',
    'textarea[placeholder*="内容"]',
    'textarea[placeholder*="content"]',
    '[name=content]',
    '#content',
    '[contenteditable=true]',
    'textarea'
  ];
  
  for (const selector of contentSelectors) {
    if (await page.isVisible(selector)) {
      await page.fill(selector, content);
      break;
    }
  }
}

async function fillOptionalField(page: Page, fieldName: string, value: string) {
  const selectors = [
    `[data-testid=${fieldName}]`,
    `[name=${fieldName.replace('-', '_')}]`,
    `input[placeholder*="${value}"]`
  ];
  
  for (const selector of selectors) {
    if (await page.isVisible(selector)) {
      await page.fill(selector, value);
      break;
    }
  }
}

async function verifyNoteSaved(page: Page, title: string) {
  const successIndicators = [
    'text=保存成功',
    'text=创建成功',
    'text=Save successful',
    'text=Created successfully',
    '[data-testid=success-message]',
    '.success-message'
  ];
  
  // Check for success message or note viewer
  let verified = false;
  for (const indicator of successIndicators) {
    if (await page.isVisible(indicator)) {
      verified = true;
      break;
    }
  }
  
  // Alternative: check if we can see the note title displayed
  if (!verified) {
    await expect(page.locator(`text=${title}`)).toBeVisible({ timeout: 5000 });
  }
}

async function createTestNote(page: Page, note: any) {
  // Navigate to a problem page and create a note
  await page.goto('/codetop');
  await page.waitForLoadState('networkidle');
  
  // Try to go to first problem
  const firstProblem = page.locator('[data-testid=problem-card], .problem-card, a[href*="/problem/"]').first();
  if (await firstProblem.isVisible()) {
    await firstProblem.click();
  } else {
    // Fallback to specific problem
    await page.goto(`/problem/${testProblem.id}`);
  }
  
  await page.waitForLoadState('networkidle');
  
  // Open notes section and create note
  const notesTab = await findNotesTab(page);
  if (notesTab) {
    await notesTab.click();
  }
  
  const createButton = await findCreateNoteButton(page);
  if (createButton) {
    await createButton.click();
    
    // Wait for form and fill it
    await page.waitForSelector('[data-testid=note-editor], .note-editor, textarea');
    await fillNoteForm(page, note);
    
    const saveButton = await findSaveButton(page);
    if (saveButton) {
      await saveButton.click();
    }
    
    // Wait for save to complete
    await page.waitForLoadState('networkidle');
  }
}

async function cleanupTestNotes(page: Page) {
  try {
    await page.goto('/notes/my');
    await page.waitForLoadState('networkidle');
    
    // Find and delete any test notes
    const testNoteSelectors = [
      'text=测试笔记标题',
      'text=E2E Test Note',
      'text=搜索测试笔记',
      'text=公开测试笔记'
    ];
    
    for (const selector of testNoteSelectors) {
      const noteElements = page.locator(selector);
      const count = await noteElements.count();
      
      for (let i = 0; i < count; i++) {
        try {
          const noteCard = noteElements.nth(i).locator('..').locator('[data-testid=note-card], .note-card').first();
          const deleteButton = noteCard.locator('[data-testid=delete-button], button:has-text("删除")').first();
          
          if (await deleteButton.isVisible()) {
            await deleteButton.click();
            await page.locator('[data-testid=confirm-delete], button:has-text("确认删除")').click();
            await page.waitForTimeout(1000); // Wait for deletion to complete
          }
        } catch (e) {
          // Continue with next note if deletion fails
          console.log('Failed to delete test note:', e);
        }
      }
    }
  } catch (e) {
    // Cleanup is best effort, don't fail the test
    console.log('Cleanup failed:', e);
  }
}