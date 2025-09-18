# AI Recommendation System Test Suite

This document describes the comprehensive test suite for the AI recommendation system implementation.

## Test Structure

### Unit Tests (`__tests__/`)

Located in `__tests__/` directory with Vitest + React Testing Library setup.

#### 1. Infinite Loading Logic (`AIRecommendationsInfiniteList.test.tsx`)

**Purpose**: Validates the `getNextPageParam` fallback chain for pagination.

**Key Test Cases**:
- ✅ **Primary Strategy**: Uses `nextCursor` when available
- ✅ **Fallback Strategy**: Uses `hasMore + nextPage` when `nextCursor` is missing
- ✅ **Termination**: Returns `undefined` when no more pages available
- ✅ **Graceful Handling**: Handles missing pagination metadata
- ✅ **Banner Aggregation**: Shows busy/DEFAULT banner when any page triggers condition

**Critical Logic Tested**:
```typescript
const nextPageParam = lastPage.data.meta.nextCursor || 
  (lastPage.data.meta.hasMore && lastPage.data.meta.nextPage ? 
    lastPage.data.meta.nextPage.toString() : undefined)
```

#### 2. Badge Visibility (`AIBadge.test.tsx`)

**Purpose**: Validates AIBadge component visibility logic and context integration.

**Key Test Cases**:
- ✅ **Conditional Rendering**: Shows badge only when problem is recommended AND AB test allows
- ✅ **AB Testing Respect**: No badge when `shouldShowAIRecommendations()` returns false
- ✅ **Context Integration**: Properly accesses and responds to RecommendationContext
- ✅ **Graceful Degradation**: Handles missing context without crashing
- ✅ **Size Variants**: Renders correctly with different size props

#### 3. Prefetch Control (`useRecommendationsPrefetch.test.tsx`)

**Purpose**: Validates prefetch hook's enabled control logic and context updates.

**Key Test Cases**:
- ✅ **Disabled State**: No API calls when `enabled=false`
- ✅ **AB Testing**: No prefetch when `shouldShowAIRecommendations()` returns false
- ✅ **Successful Prefetch**: Makes API call with correct parameters when enabled
- ✅ **Default Values**: Uses `enabled=true` and `limit=10` as defaults
- ✅ **Context Updates**: Adds prefetched problem IDs to recommendation context
- ✅ **Query Configuration**: Uses appropriate stale time and cache settings

### E2E Tests (`tests/e2e/`)

Located in `tests/e2e/` directory with Playwright setup.

#### 1. Infinite Loading Scenarios (`ai-recommendations-infinite.spec.ts`)

**Purpose**: End-to-end validation of infinite scrolling behavior.

**Key Test Cases**:
- ✅ **Cursor Strategy**: Loads pages using `nextCursor` with auto-scroll
- ✅ **Page Fallback**: Handles page-based pagination when cursor unavailable
- ✅ **Error Handling**: Shows error message while preserving loaded items
- ✅ **Loading States**: Displays spinner during loading
- ✅ **Termination**: Shows "已加载全部推荐" when no more pages

**User Scenarios Tested**:
- User scrolls to bottom → Next page loads automatically
- Backend only supports page-based pagination → Fallback works
- Network error during loading → Error message shown, existing items preserved

#### 2. Badge Visibility (`ai-badge-visibility.spec.ts`)

**Purpose**: Validates badge display across different pages without manual navigation to recommendations.

**Key Test Cases**:
- ✅ **Home Page**: Badges appear immediately on problem list (prefetch working)
- ✅ **Codetop Table**: Badges appear in table view without visiting recommendations page
- ✅ **Navigation Persistence**: Badges remain visible when navigating between pages
- ✅ **Prefetch Failure**: Graceful handling when prefetch API fails
- ✅ **AB Testing**: Respects AB test settings (no badges when disabled)
- ✅ **Context Updates**: Badges update when recommendation state changes

**User Scenarios Tested**:
- User visits home page first → AI badges immediately visible
- User goes directly to codetop page → Badges appear via prefetch
- User navigates between pages → Context state persists

#### 3. Busy/DEFAULT Banner (`ai-recommendations-banner.spec.ts`)

**Purpose**: Validates system status banner display logic across different scenarios.

**Key Test Cases**:
- ✅ **Meta Busy**: Banner appears when any page has `meta.busy=true`
- ✅ **DEFAULT Source**: Banner appears when `rec-source` header is "DEFAULT"
- ✅ **Multiple Conditions**: Banner appears when both conditions are met
- ✅ **Normal Operation**: No banner when system operates with LLM source
- ✅ **Infinite Aggregation**: Banner appears when ANY page in infinite loading triggers condition
- ✅ **Refresh Persistence**: Banner state persists when refreshing recommendations

**System States Tested**:
- Normal: LLM source, `busy=false` → No banner
- Busy: Any source, `busy=true` → Banner shown
- Fallback: DEFAULT source, any busy state → Banner shown

## Running Tests

### Unit Tests

```bash
# Run all unit tests
npm run test

# Run with coverage
npm run test:coverage

# Run with interactive UI
npm run test:ui

# Run unit tests only (one-time)
npm run test:run

# Quick script
./run-unit-tests.sh
```

### E2E Tests

```bash
# Run all E2E tests
npm run test:e2e

# Run AI recommendation specific tests
./run-e2e-ai-tests.sh

# Run with UI mode
npm run test:e2e:ui

# View test results
npm run test:e2e:report
```

## Test Configuration

### Vitest Setup (`vitest.config.ts`)

- **Environment**: jsdom for browser simulation
- **Globals**: Enabled for describe/it/expect
- **Setup**: Custom setup file with mocks
- **Coverage**: v8 provider with HTML reports

### Key Mocks (`vitest.setup.ts`)

- Next.js router and navigation
- IntersectionObserver for infinite scroll
- LocalStorage/SessionStorage
- Window.matchMedia for responsive design

### Playwright Configuration

- **Browsers**: Chromium, Firefox, Safari
- **Base URL**: Configurable for different environments
- **Auth Helper**: Reusable authentication utilities
- **API Mocking**: Comprehensive request/response mocking

## Verification Scenarios

### How to Verify Implementation

#### 1. Infinite Loading

```bash
# Terminal 1: Start development server
npm run dev

# Terminal 2: Run infinite loading tests
npx playwright test ai-recommendations-infinite.spec.ts --headed
```

**Manual Verification**:
1. Go to `/dashboard/recommendations`
2. Enable infinite loading mode
3. Scroll to bottom → Next page loads automatically
4. Verify different pagination strategies work

#### 2. Badge Coverage

```bash
# Run badge visibility tests
npx playwright test ai-badge-visibility.spec.ts --headed
```

**Manual Verification**:
1. Clear browser cache/localStorage
2. Go directly to home page (`/`) → Badges appear immediately
3. Go directly to codetop (`/codetop`) → Badges appear without visiting recommendations
4. Navigate between pages → Badges persist

#### 3. Busy/DEFAULT Banner

```bash
# Run banner tests
npx playwright test ai-recommendations-banner.spec.ts --headed
```

**Manual Verification**:
1. Mock backend to return `rec-source: DEFAULT` header
2. Go to recommendations page → Yellow warning banner appears
3. Check dev debug info → "来源: DEFAULT" shown

## Coverage Targets

### Unit Test Coverage

- **Lines**: >90%
- **Functions**: >90% 
- **Branches**: >85%
- **Statements**: >90%

### E2E Test Coverage

- **User Journeys**: 100% of critical paths
- **Error Scenarios**: Network failures, API errors
- **Cross-browser**: Chrome, Firefox, Safari
- **Responsive**: Desktop and mobile viewports

## Test Data

### Mock API Responses

All tests use consistent mock data patterns:

```typescript
// Standard recommendation item
{
  problemId: number,
  title: string,
  reason: string,
  confidence: number, // 0.0 - 1.0
  source: 'LLM' | 'FSRS' | 'HYBRID' | 'DEFAULT',
  difficulty?: 'easy' | 'medium' | 'hard',
  topics?: string[],
  latencyMs?: number
}

// Pagination metadata
{
  nextCursor?: string,      // Primary strategy
  hasMore?: boolean,        // Fallback strategy
  nextPage?: number,        // Fallback strategy
  busy?: boolean,           // Banner trigger
  totalItems?: number
}
```

### Headers Testing

```typescript
// Response headers that affect behavior
{
  'rec-source': 'LLM' | 'FSRS' | 'HYBRID' | 'DEFAULT', // Banner logic
  'cache-hit': 'true' | 'false',                       // Cache indicator
  'provider-chain': string                              // Debug info
}
```

## Debugging Tests

### Unit Test Debugging

```bash
# Run specific test file
npm run test AIRecommendationsInfiniteList.test.tsx

# Run with debug output
npm run test -- --reporter=verbose

# Run single test case
npm run test -- -t "should use nextCursor when available"
```

### E2E Test Debugging

```bash
# Run with browser visible
npx playwright test --headed

# Debug mode (step through)
npx playwright test --debug

# Trace mode (full recording)
npx playwright test --trace on
```

### Common Issues

1. **Mock API not responding**: Check route patterns match exactly
2. **Context not updating**: Verify RecommendationProvider wraps components
3. **Infinite scroll not triggering**: Check IntersectionObserver mock setup
4. **Authentication required**: Use AuthHelper in E2E tests

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/test.yml
- name: Run Unit Tests
  run: npm run test:run

- name: Run E2E Tests  
  run: npm run test:e2e

- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

### Quality Gates

- Unit tests must pass with >90% coverage
- E2E tests must pass on all target browsers
- No console errors during test execution
- Performance budgets maintained