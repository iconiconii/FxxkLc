# LLM智能推荐题目功能开发

LLM Service Integration
[ ] AI Recommendation Service
  - [ ] Create `AIRecommendationService` class in `service/` package with LLM client integration
  - [ ] Implement OpenAI GPT-4 or alternative LLM API client with async support
  - [ ] Design prompt engineering templates for recommendation generation
  - [ ] Add retry mechanism, timeout control, and graceful error handling
  - [ ] Configure rate limiting to prevent API quota exhaustion

[ ] Configuration Management
  - [ ] Add LLM configuration section to `application.yml` (API keys, endpoints, models)
  - [ ] Support multi-environment configs (dev/test/prod) with different LLM providers
  - [ ] Implement secure API key management via environment variables
  - [ ] Add feature toggles for LLM recommendation on/off per user segment

Assumptions / Constraints / Non-goals
- Assumptions: LLM API access available; existing FSRS data sufficient for user profiling
- Constraints: LLM API cost budget; response time < 2s; backward compatibility with existing recommendation
- Non-goals: Real-time chat interface; content generation; replacing FSRS algorithm entirely

Open Questions
- Which LLM provider offers best cost/performance for recommendation tasks?
- Should we support multiple LLM models simultaneously for A/B testing?
- How to handle LLM API outages without degrading user experience?

Acceptance Criteria
- [ ] `AIRecommendationService` successfully calls LLM API and parses responses
- [ ] Configuration supports multiple environments and secure credential management
- [ ] Service gracefully handles API failures with fallback to FSRS recommendations
- [ ] Rate limiting prevents API quota overruns

Commit Message
feat(ai): add LLM service integration for intelligent recommendations
- Implement AIRecommendationService with OpenAI GPT-4 client
- Add multi-environment configuration and secure API key management
- Include retry logic, rate limiting, and fallback mechanisms

---

User Profiling & Algorithm
[ ] User Profile Analysis
  - [ ] Analyze `FSRSCard` and `ReviewLog` data to extract learning patterns
  - [ ] Calculate skill levels per knowledge domain (arrays, graphs, dynamic programming, etc.)
  - [ ] Identify weak areas based on review performance and retention rates
  - [ ] Build user preference vectors from problem interaction history

[ ] Problem Feature Enhancement  
  - [ ] Add `content` TEXT field to `Problem` entity for detailed descriptions
  - [ ] Extract knowledge points, algorithm types, and complexity features from problem content
  - [ ] Implement problem similarity scoring based on tags, difficulty, and content analysis
  - [ ] Create problem categorization mapping (topic → subtopic → specific algorithms)

[ ] Intelligent Recommendation Engine
  - [ ] Design prompt templates combining user profile + problem features + learning objectives
  - [ ] Implement hybrid algorithm merging FSRS scheduling with LLM content-based recommendations
  - [ ] Support multi-dimensional recommendation strategies (progressive difficulty, knowledge coverage, personalization)
  - [ ] Add confidence scoring for recommendation quality assessment

Assumptions / Constraints / Non-goals
- Assumptions: Problem content data available or can be enriched; user has sufficient learning history
- Constraints: Algorithm complexity manageable within response time limits; recommendation diversity maintained
- Non-goals: Automatic problem content generation; real-time collaborative filtering

Open Questions
- How many historical reviews needed for reliable user profiling?
- Should recommendation confidence scores be exposed to users?
- How to balance exploration vs exploitation in recommendation diversity?

Acceptance Criteria
- [ ] User profiles accurately reflect learning strengths and weaknesses
- [ ] Problem similarity scoring produces meaningful content-based groupings
- [ ] LLM recommendations complement rather than conflict with FSRS scheduling
- [ ] Recommendation quality measurably improves over baseline methods

Commit Message  
feat(algorithm): implement intelligent user profiling and hybrid recommendation engine
- Add user skill analysis based on FSRS review history
- Enhance Problem entity with content and feature extraction
- Merge LLM content recommendations with FSRS temporal scheduling

---

API & Caching Layer
[ ] Recommendation APIs
  - [ ] Add `GET /api/v1/problems/ai-recommendations` endpoint in `ProblemController`
  - [ ] Implement `POST /api/v1/problems/{id}/recommendation-feedback` for user feedback collection
  - [ ] Support request parameters (limit, difficulty_preference, topic_filter, recommendation_type)
  - [ ] Create response DTOs with recommendation reasons and confidence scores
  - [ ] Integrate AI recommendation toggle into existing `getRecommendedProblems` method

[ ] Caching Strategy
  - [ ] Implement Redis caching for user profiles with configurable TTL (1 hour default)
  - [ ] Cache LLM recommendation results with composite keys (user_id + preferences_hash)
  - [ ] Add cache invalidation triggers on user learning behavior changes
  - [ ] Optimize cache hit rates through intelligent pre-loading for active users

Assumptions / Constraints / Non-goals
- Assumptions: Redis cluster available; API response formats backward compatible
- Constraints: Cache memory usage reasonable; API latency targets maintained
- Non-goals: Real-time recommendation updates; complex cache warming strategies

Open Questions
- Optimal cache TTL balancing freshness vs performance?
- Should recommendation feedback immediately invalidate user cache?
- How to handle cache consistency across multiple app instances?

Acceptance Criteria
- [ ] AI recommendation API returns personalized results within latency targets
- [ ] Caching reduces LLM API calls by >80% for repeat requests
- [ ] User feedback properly collected and stored for future algorithm improvements
- [ ] Backward compatibility maintained with existing recommendation flows

Commit Message
feat(api): add AI recommendation endpoints with intelligent caching
- Implement personalized recommendation API with user feedback collection
- Add Redis-based caching for user profiles and LLM responses
- Maintain backward compatibility with existing recommendation system

---

Frontend Integration
[ ] UI Components
  - [ ] Create `AIRecommendationCard` React component for displaying recommended problems
  - [ ] Implement recommendation explanation UI showing reasoning and confidence
  - [ ] Add user feedback buttons (helpful/not helpful/already mastered) with analytics tracking
  - [ ] Design loading states and error handling for LLM recommendation delays

[ ] Dashboard Integration
  - [ ] Add "AI Recommendations" section to user dashboard with personalized suggestions
  - [ ] Implement toggle between traditional FSRS and AI-powered recommendations
  - [ ] Create recommendation history view showing past suggestions and outcomes
  - [ ] Add recommendation settings page for user preferences (topics, difficulty, frequency)

[ ] Problem List Enhancement
  - [ ] Integrate AI recommendation indicator in problem listing pages
  - [ ] Implement lazy loading and pagination for recommendation results
  - [ ] Add recommendation filters and sorting options
  - [ ] Create recommendation quality indicators (match score, confidence level)

Assumptions / Constraints / Non-goals
- Assumptions: Next.js frontend structure allows component integration; design system available
- Constraints: Page load performance maintained; mobile responsiveness required
- Non-goals: Complete UI redesign; complex recommendation visualization

Open Questions
- How detailed should recommendation explanations be for users?
- Should recommendation preferences be per-user or per-session?
- What's the ideal recommendation refresh frequency for active users?

Acceptance Criteria
- [ ] AI recommendation components render properly across devices and browsers
- [ ] User feedback collection works and data flows to backend analytics
- [ ] Dashboard integration enhances rather than clutters existing user experience
- [ ] Recommendation UI clearly communicates value and reasoning to users

Commit Message
feat(frontend): add AI recommendation UI components and dashboard integration
- Create AIRecommendationCard with explanation and feedback functionality
- Integrate AI recommendations into dashboard and problem listings
- Add user preference controls and recommendation history views

---

Testing & Monitoring
[ ] Automated Testing
  - [ ] Unit tests for `AIRecommendationService` including LLM API mocking
  - [ ] Integration tests for recommendation algorithms with test user data
  - [ ] API contract tests for all new recommendation endpoints
  - [ ] Frontend component tests for AI recommendation UI elements

[ ] Performance & Quality Monitoring
  - [ ] Add metrics collection for LLM API response times and success rates
  - [ ] Implement recommendation quality scoring based on user engagement
  - [ ] Set up alerting for LLM service degradation or high error rates
  - [ ] Create recommendation analytics dashboard for system administrators

[ ] Load Testing & Optimization
  - [ ] Performance tests for recommendation endpoints under realistic load
  - [ ] Cache hit rate optimization and memory usage monitoring
  - [ ] LLM API quota usage tracking and cost optimization
  - [ ] Recommendation accuracy evaluation with A/B testing framework

Assumptions / Constraints / Non-goals
- Assumptions: Testing infrastructure supports API mocking; monitoring tools available
- Constraints: Test execution time reasonable; monitoring overhead minimal
- Non-goals: Manual testing procedures; complex ML model evaluation frameworks

Open Questions
- What metrics best indicate recommendation quality beyond user feedback?
- Should we implement automated rollback for poor-performing recommendation models?
- How to balance comprehensive testing with development velocity?

Acceptance Criteria
- [ ] All automated tests pass with >90% code coverage for new functionality
- [ ] Performance monitoring shows recommendation endpoints meet latency targets
- [ ] LLM API usage stays within budget constraints with alerting for overruns
- [ ] A/B testing framework validates recommendation quality improvements

Commit Message
feat(testing): add comprehensive testing and monitoring for AI recommendations
- Implement unit, integration, and performance tests for recommendation system
- Add metrics collection and alerting for LLM service health
- Create recommendation quality evaluation and A/B testing framework