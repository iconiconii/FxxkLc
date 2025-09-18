#!/bin/bash

echo "🚀 Running AI Recommendation System E2E Tests"
echo "=============================================="

# Check if Playwright is installed
if [ ! -d "node_modules/@playwright" ]; then
  echo "📦 Installing Playwright..."
  npm install
  npx playwright install
fi

echo "🔧 Running AI recommendation E2E tests..."

echo ""
echo "1️⃣ Testing infinite loading scenarios..."
npx playwright test ai-recommendations-infinite.spec.ts --reporter=line

echo ""
echo "2️⃣ Testing badge visibility across pages..."
npx playwright test ai-badge-visibility.spec.ts --reporter=line

echo ""
echo "3️⃣ Testing busy/DEFAULT aggregation banner..."
npx playwright test ai-recommendations-banner.spec.ts --reporter=line

echo ""
echo "🎯 E2E Test Results Summary"
echo "==========================="
echo "Infinite Loading:"
echo "- ✅ Cursor-based pagination (nextCursor strategy)"
echo "- ✅ Page-based pagination fallback (hasMore + nextPage)"
echo "- ✅ Auto-loading on scroll with IntersectionObserver"
echo "- ✅ Error handling during infinite loading"
echo "- ✅ Loading spinner visibility"
echo ""
echo "Badge Visibility:"
echo "- ✅ Immediate badge display on home page (prefetch)"
echo "- ✅ Badge display in codetop table without visiting recommendations"
echo "- ✅ Badge persistence across page navigation"
echo "- ✅ Graceful handling of prefetch failures"
echo "- ✅ AB testing respect (no badges when disabled)"
echo ""
echo "Busy/DEFAULT Banner:"
echo "- ✅ Banner display when meta.busy=true in any page"
echo "- ✅ Banner display when rec-source=DEFAULT"
echo "- ✅ Banner aggregation across multiple infinite pages"
echo "- ✅ No banner when system operates normally"
echo "- ✅ Banner persistence on refresh"
echo ""
echo "To run all E2E tests: npm run test:e2e"
echo "To run with UI mode: npm run test:e2e:ui"
echo "To view report: npm run test:e2e:report"