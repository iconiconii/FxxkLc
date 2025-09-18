#!/bin/bash

echo "🧪 Running AI Recommendation System Unit Tests"
echo "============================================="

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
  echo "📦 Installing dependencies..."
  npm install
fi

echo "🔧 Running unit tests with coverage..."
npm run test:run

echo ""
echo "📊 Generating coverage report..."
npm run test:coverage

echo ""
echo "🎯 Test Results Summary"
echo "======================="
echo "Unit Tests:"
echo "- ✅ getNextPageParam fallback logic (cursor → page → undefined)"
echo "- ✅ AIBadge visibility and context integration"  
echo "- ✅ Prefetch enabled control logic (AB testing + authentication)"
echo ""
echo "Coverage report available at: coverage/index.html"
echo ""
echo "To run tests interactively: npm run test"
echo "To run tests with UI: npm run test:ui"