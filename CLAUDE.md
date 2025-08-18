# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CodeTop FSRS Backend is an intelligent algorithm problem management system built with Java 17 + Spring Boot 3.2.1. The system integrates the FSRS (Free Spaced Repetition Scheduler) v4.5+ algorithm for personalized learning optimization, featuring JWT authentication, Redis caching, and comprehensive user analytics.

The project consists of:
- **Backend**: Java Spring Boot API server with FSRS algorithm integration
- **Frontend**: Next.js 15 React application with Radix UI components
- **Database**: MySQL 8.0 with Redis 7 caching layer

## Development Commands

### Backend (Spring Boot)

```bash
# Start the application (requires Docker containers running)
mvn spring-boot:run

# Build the project
mvn clean compile

# Package for deployment
mvn clean package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn spring-boot:run -Dspring-boot.run.profiles=test
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Frontend (Next.js)

```bash
# Navigate to frontend directory first
cd frontend/

# Start development server with Turbopack
npm run dev

# Build for production
npm run build

# Start production server
npm run start

# Run linting
npm run lint
```

### Testing

```bash
**important** 测试账号 2441933762@qq.com password: password123_
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest="**/*UnitTest"        # Unit tests only
mvn test -Dtest="**/*IntegrationTest" # Integration tests only  
mvn test -Dtest="**/*SecurityTest"    # Security tests only
mvn test -Dtest="**/*PerformanceTest" # Performance tests only

# Run complete test suite
mvn test -Dtest=TestSuite

# Run tests with specific profiles
mvn test -Ptest
mvn test -Pperformance -Dtest="**/*PerformanceBenchmarkTest"
mvn test -Psecurity -Dtest="**/*SecurityTest"

# Generate coverage report
mvn clean test jacoco:report
# View at: target/site/jacoco/index.html
```

### Database Operations
redis容器: codetop-redis
mysql容器: codetop-mysql
```bash
# Run Flyway migrations
**important** Use mysql mcp when need to change database

# Database setup with Docker
docker run --name codetop-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=codetop_fsrs \
  -p 3306:3306 -d mysql:8.0

# Load test data
mysql -u root -p codetop_fsrs < test-data.sql
```

### Development Profiles

- **dev**: Development with debug logging, localhost CORS, development JWT secret
- **test**: Testing profile with separate test database and Redis database 1
- **prod**: Production with strict security requirements and environment variables

## Architecture Overview

### Core System Architecture

The system follows a layered architecture with clear separation of concerns:

**Application Layer**:
- `CodetopFsrsApplication.java` - Main Spring Boot application with caching, async processing, and scheduling enabled
- Annotations: `@EnableCaching`, `@EnableAsync`, `@EnableScheduling`, `@MapperScan`

**API Layer** (`controller/`):
- `AuthController` - JWT authentication and OAuth2 (GitHub/Google) integration
- `ProblemController` - Algorithm problem CRUD operations and search
- `ReviewController` - FSRS review queue generation and review processing
- `AnalyticsController` - User progress tracking and dashboard analytics
- REST endpoints with OpenAPI 3.0 documentation at `/swagger-ui.html`

**Business Logic Layer** (`service/`):
- `FSRSService` - Core FSRS algorithm implementation with 17-parameter model
- `AuthService` - Authentication, JWT token management, OAuth2 integration
- `ProblemService` - Problem management with company associations and difficulty levels
- `UserParametersService` - FSRS parameter optimization with gradient descent algorithm

**Data Access Layer** (`mapper/`):
- MyBatis-Plus mappers for optimized database operations
- Custom SQL in `mapper/**/*.xml` for complex queries
- Logical foreign key relationships (no physical constraints)

**Domain Models** (`entity/`):
- `User` - User accounts with OAuth support and preferences
- `Problem` - Algorithm problems with difficulty and frequency metadata  
- `FSRSCard` - Spaced repetition cards with state machine (NEW → LEARNING → REVIEW → RELEARNING)
- `ReviewLog` - Complete learning history for parameter optimization
- `UserParameters` - Personalized 17-parameter FSRS configuration per user

### FSRS Algorithm Integration

The system implements FSRS v4.5+ with complete 17-parameter model:

**Core Algorithm** (`algorithm/FSRSAlgorithmImpl`):
- Stability and difficulty calculations
- Next review interval prediction
- State transitions and retrievability modeling
- Sub-10ms calculation performance target

**Parameter Optimization**:
- Gradient descent optimization requiring minimum 50 reviews
- Batch processing for large datasets
- Redis caching for optimized performance
- Target retention rate: 90%

**Configuration** (application.yml):
```yaml
fsrs:
  default-parameters: [0.4, 0.6, 2.4, 5.8, 4.93, ...]  # 17 parameters
  target-retention: 0.9
  maximum-interval: 365
  optimization:
    minimum-reviews: 50
    learning-rate: 0.001
```

### Security Architecture

**Authentication**:
- JWT tokens with RS256 signing
- OAuth2 integration (GitHub, Google)
- Token refresh mechanism
- User session management

**Authorization**:
- Role-based access control
- Resource ownership validation
- Request rate limiting (100 req/min per user, 1000 req/min per IP)

**Security Features**:
- Input sanitization and validation
- SQL injection prevention
- XSS protection
- CORS policy enforcement
- Security audit logging

**Production Security Requirements**:
- JWT_SECRET must be 256+ bits
- CORS_ALLOWED_ORIGINS must be explicitly configured (no wildcards)
- All OAuth credentials must be set via environment variables

### Frontend Architecture (Next.js)

**App Router Structure** (`frontend/app/`):
- `/dashboard` - User dashboard with progress analytics
- `/codetop` - Algorithm problem practice interface
- `/review` - FSRS spaced repetition review system
- `/analysis` - Learning analytics and performance insights
- `/leaderboard` - Social rankings and achievements
- `/login`, `/register` - Authentication pages

**Component System** (`frontend/components/`):
- **UI Components**: Built with Radix UI primitives (Dialog, DropdownMenu, Select, etc.)
- **Theme System**: Dark/light mode with `next-themes`
- **Layout Components**: Dashboard, sidebar navigation, responsive design
- **Domain Components**: Problem assessment, review interface, analytics visualization

**Key Dependencies**:
- Next.js 15 with App Router and Turbopack
- React 19 with TypeScript 5
- Tailwind CSS 4 with custom configuration
- Radix UI for accessibility-compliant components
- Recharts for data visualization

### Database Schema

**Migration System**: Flyway with versioned migrations in `src/main/resources/db/migration/`

**Core Tables**:
- `users` - User accounts with OAuth and preferences
- `problems` - Algorithm problems with metadata
- `companies` - Problem-to-company associations
- `fsrs_cards` - Spaced repetition cards with FSRS state
- `review_logs` - Complete review history for optimization
- `user_fsrs_parameters` - Personalized 17-parameter configurations
- `user_statistics` - Aggregated learning analytics
- `leaderboard_entries` - Social ranking system

**Design Principles**:
- Logical foreign key relationships only (no physical constraints for MyBatis-Plus compatibility)
- Soft delete pattern with `deleted` TINYINT flag
- Comprehensive indexing strategy for performance
- JSON columns for flexible user preferences and metadata

### Testing Strategy

**Test Structure** (`src/test/java/com/codetop/`):
- `TestSuite.java` - Complete test orchestration
- `algorithm/FSRSAlgorithmTest` - Mathematical correctness validation
- `service/UserParametersServiceTest` - Business logic and caching tests
- `security/SecurityIntegrationTest` - Comprehensive security validation
- `integration/FSRSWorkflowIntegrationTest` - End-to-end learning workflows
- `performance/FSRSPerformanceBenchmarkTest` - Scalability and performance validation
- `api/ApiContractTest` - Complete API contract validation

**Infrastructure**:
- TestContainers with MySQL 8.0 and Redis 7 containers
- Comprehensive test data in `test-data.sql`
- Quality target: 96/100 validation score with >90% code coverage

**Performance Targets**:
- FSRS calculations: <10ms average, <20ms P95, <50ms maximum
- Throughput: >1000 calculations/second
- API response times: <200ms for critical operations

## Important Configuration Notes

### Environment Variables (Production)

```bash
# Database
DATABASE_URL=jdbc:mysql://host:port/database
DATABASE_USERNAME=username
DATABASE_PASSWORD=password

# Redis
REDIS_URL=redis://host:port
REDIS_PASSWORD=password

# JWT Security (REQUIRED - 256+ bits)
JWT_SECRET=your_256_bit_secret_key
JWT_EXPIRATION=3600000  # 1 hour in production
JWT_REFRESH_EXPIRATION=86400000  # 24 hours

# OAuth Integration
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# CORS Security
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_PER_USER=100
RATE_LIMIT_PER_IP=1000
```

### Docker Infrastructure

```bash
# MySQL container
docker run --name codetop-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=codetop_fsrs \
  -p 3306:3306 -d mysql:8.0

```

### API Documentation

- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
- OpenAPI spec: http://localhost:8080/api/v1/api-docs
- Actuator health: http://localhost:8080/api/v1/actuator/health
- Druid monitoring: http://localhost:8080/api/v1/druid/ (dev profile only)

## Code Quality and Best Practices

- **Lombok**: Used extensively for reducing boilerplate (getters, setters, builders)
- **Validation**: Jakarta Validation with custom validators in `validation/` package
- **Exception Handling**: Global exception handler with structured error responses
- **Logging**: Structured logging with request correlation IDs
- **Caching**: Multi-layer Redis caching with TTL configuration
- **Async Processing**: Background parameter optimization with Spring @Async
- **Database**: Optimized queries with proper indexing and connection pooling
- **Security**: Comprehensive input sanitization and audit logging

The system is production-ready with comprehensive monitoring, security hardening, and scalability optimizations.