# E2E 测试指南

本文档介绍如何运行和维护 CodeTop FSRS 项目的端到端(E2E)测试。

## 测试环境设置

### 1. 安装依赖

```bash
cd frontend/
npm install
```

### 2. 安装 Playwright 浏览器

```bash
npx playwright install
```

### 3. 环境配置

确保以下服务正在运行：

```bash
# 后端服务 (端口 8080)
cd .. && mvn spring-boot:run

# 前端开发服务器 (端口 3000)  
cd frontend/ && npm run dev
```

### 4. 测试数据准备

测试使用以下账号：
- **邮箱**: 2441933762@qq.com
- **密码**: password123_

确保该测试账号在系统中存在且可正常登录。

## 运行测试

### 基础冒烟测试

```bash
# 运行简单的冒烟测试
npm run test:e2e -- --grep "基础冒烟测试"
```

### 完整测试套件

```bash
# 运行所有 E2E 测试
npm run test:e2e

# 运行特定测试文件
npm run test:e2e tests/e2e/notes.spec.ts
npm run test:e2e tests/e2e/auth.spec.ts
npm run test:e2e tests/e2e/navigation.spec.ts

# 使用 UI 模式运行测试
npm run test:e2e:ui

# 查看测试报告
npm run test:e2e:report
```

### 浏览器选择

```bash
# 只在 Chrome 中运行
npm run test:e2e -- --project=chromium

# 只在 Firefox 中运行  
npm run test:e2e -- --project=firefox

# 移动端测试
npm run test:e2e -- --project="Mobile Chrome"
```

## 测试文件结构

```
tests/e2e/
├── README.md                 # 本文档
├── global-setup.ts          # 全局测试设置
├── fixtures/                # 测试辅助文件
│   ├── test-data.ts         # 测试数据
│   └── auth-helper.ts       # 认证辅助类
├── simple-smoke.spec.ts     # 基础冒烟测试
├── auth.spec.ts             # 认证功能测试
├── notes.spec.ts            # 笔记功能测试
└── navigation.spec.ts       # 导航功能测试
```

## 测试范围

### 1. 认证测试 (auth.spec.ts)
- ✅ 用户登录流程
- ✅ 错误凭据处理
- ✅ 用户注册流程
- ✅ 用户登出流程
- ✅ 未认证访问重定向
- ✅ 会话超时处理

### 2. 笔记功能测试 (notes.spec.ts)
- ✅ 创建笔记完整流程
- ✅ 编辑现有笔记
- ✅ 删除笔记
- ✅ 公开笔记浏览和投票
- ✅ 笔记搜索功能
- ✅ 笔记标签功能

### 3. 导航测试 (navigation.spec.ts)
- ✅ 主要页面导航
- ✅ 侧边栏导航
- ✅ 面包屑导航
- ✅ 浏览器历史导航
- ✅ 搜索导航
- ✅ 响应式移动导航
- ✅ 键盘导航

### 4. 基础冒烟测试 (simple-smoke.spec.ts)
- ✅ 应用程序启动检查
- ✅ 登录页面可访问性
- ✅ 基础页面响应检查

## 测试特性

### 智能元素选择
测试使用多种选择器策略，确保在不同实现中都能正常工作：

```typescript
// 示例：查找登录按钮
const loginButtonSelectors = [
  '[data-testid=login-button]',    // 推荐的测试ID
  'button[type=submit]',            // 语义化选择器
  'button:has-text("登录")',        // 文本内容选择器
  'button:has-text("Login")',       // 英文文本
  '.login-btn'                      // CSS类名备选
];
```

### 自动清理
测试会在运行后自动清理创建的测试数据，避免影响后续测试。

### 错误容错
测试具有良好的容错性，当某些功能不可用时会跳过相关测试而非失败。

## 持续集成配置

### GitHub Actions 示例

```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: codetop_fsrs
        ports:
          - 3306:3306
          
      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Install dependencies
      run: |
        cd frontend && npm install
        
    - name: Install Playwright
      run: |
        cd frontend && npx playwright install --with-deps
        
    - name: Start backend
      run: |
        mvn spring-boot:run -Dspring-boot.run.profiles=test &
        sleep 30
        
    - name: Start frontend
      run: |
        cd frontend && npm run build && npm run start &
        sleep 15
        
    - name: Run E2E tests
      run: |
        cd frontend && npm run test:e2e
        
    - name: Upload test reports
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: playwright-report
        path: frontend/playwright-report/
```

## 调试指南

### 1. 查看测试执行过程

```bash
# 以头模式运行测试（可看到浏览器）
npm run test:e2e -- --headed

# 启用调试模式
npm run test:e2e -- --debug

# 慢速执行便于观察
npm run test:e2e -- --slowMo=1000
```

### 2. 截图和录制

测试失败时会自动截图和录制视频，文件保存在：
- `test-results/` - 测试结果和截图
- `playwright-report/` - HTML报告

### 3. 常见问题

#### 测试超时
```bash
# 增加超时时间
npm run test:e2e -- --timeout=60000
```

#### 元素查找失败
检查页面是否正确加载，元素选择器是否正确：

```typescript
// 调试元素查找
console.log(await page.locator('selector').count());
await page.screenshot({ path: 'debug.png', fullPage: true });
```

#### 认证问题
确保测试账号存在且密码正确：

```typescript
// 检查登录状态
const isLoggedIn = await page.isVisible('[data-testid=user-menu]');
console.log('Login status:', isLoggedIn);
```

## 最佳实践

### 1. 测试数据管理
- 使用固定的测试账号
- 测试后清理创建的数据
- 使用有意义的测试数据前缀

### 2. 等待策略
```typescript
// 好的做法：等待网络空闲
await page.waitForLoadState('networkidle');

// 好的做法：等待特定元素
await page.waitForSelector('[data-testid=element]');

// 避免：固定时间等待
// await page.waitForTimeout(5000);
```

### 3. 选择器优先级
1. `[data-testid=xxx]` - 专用测试ID（推荐）
2. 语义化属性 - `input[type=email]`, `button[type=submit]`
3. 文本内容 - `text=登录`, `:has-text("确定")`
4. CSS类名 - 作为最后备选

### 4. 错误处理
```typescript
// 容错处理示例
try {
  await page.click('[data-testid=button]');
} catch (error) {
  // 尝试备选方案
  await page.click('button:has-text("确定")');
}
```

## 性能监控

测试会监控页面加载性能：

```typescript
// 性能检查示例
test('页面加载性能', async ({ page }) => {
  const start = Date.now();
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');
  const loadTime = Date.now() - start;
  
  expect(loadTime).toBeLessThan(5000); // 5秒内加载完成
});
```

## 报告和分析

### HTML 报告
运行测试后查看详细报告：
```bash
npm run test:e2e:report
```

### JSON 报告
JSON格式的测试结果保存在 `test-results/results.json`，可用于CI/CD集成。

### 测试覆盖率
虽然E2E测试主要关注功能验证，但也应该确保：
- 主要用户流程覆盖率 100%
- 关键业务功能覆盖率 100%  
- 错误处理场景覆盖率 > 80%

---

## 联系和支持

如果遇到测试相关问题：

1. 检查本文档的调试指南
2. 查看 `playwright-report/` 中的详细报告
3. 查看控制台输出和错误日志
4. 确认所有依赖服务正常运行

测试是保证代码质量的重要手段，请在提交代码前确保所有E2E测试通过。