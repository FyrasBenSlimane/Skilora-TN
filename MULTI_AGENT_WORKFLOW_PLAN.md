# Skilora Tunisia — Multi-Agent Workflow Optimization Plan

> Generated: February 9, 2026 | Project: JavaFX Desktop App (Talent Management) | Team: 6 devs

---

## 1. Executive Summary

**Skilora Tunisia** is a JavaFX 21 desktop application for talent management targeting Tunisian job seekers, built with Java 17, Maven, MySQL (HikariCP), and a custom UI component framework ("TL*" components resembling shadcn/ui). The project follows MVC with FXML views and has grown to **~60+ Java files, 15K+ LOC, 47 custom UI components**, covering 6 functional modules (Profiles, Formations, Recruitment, Support, Finance, Community).

**Current State: Fair — strong SQL injection prevention and password hashing, but zero test coverage, several critical security gaps (plaintext password fallback, hardcoded credentials), a 905-line god class (`MainView.java`), no DAO layer, and pervasive singleton coupling.** The architecture is functional but accumulating technical debt that will block the team as the 6 modules grow.

**The plan below organizes 6 weeks of improvements into 5 phases, with tasks assigned to LOCAL, BACKGROUND, and CLOUD agents running in parallel workstreams. This lets a solo developer (or small team) systematically improve the codebase while continuing feature work.**

---

## 2. Detailed Analysis

### 2.1 Project Overview

| Attribute | Value |
|---|---|
| **Type** | JavaFX Desktop Application |
| **Java** | 17 (compiles) / JavaFX 21 (runtime) |
| **Build** | Maven 3.x |
| **Database** | MySQL via HikariCP connection pool |
| **Auth** | BCrypt + Biometric (face recognition via Python) |
| **Architecture** | MVC (FXML views + Controllers + Service layer) |
| **UI Framework** | Custom "TL*" components (47 components) |
| **LOC** | ~15,000+ Java, ~21 FXML views, 3 CSS files |
| **Modules** | 21 controllers, 7 services, 10 entities, 4 enums |
| **External** | Python face recognition, job feed crawler |

### 2.2 Code Quality Assessment

#### Critical Issues (Fix Immediately)
| # | Issue | File | Impact |
|---|---|---|---|
| 1 | **Zero test coverage** — no `src/test/`, no JUnit/Mockito in pom.xml | Project-wide | Regressions undetectable |
| 2 | **Plaintext password fallback** in `verifyPassword()` | `UserService.java` L59 | Authentication bypass |
| 3 | **Resource leak** — PreparedStatement/ResultSet not closed | `JobService.java` L401-420 | Connection pool exhaustion |
| 4 | **Hardcoded DB credentials** (root/empty password) | `DatabaseConfig.java` L19-21 | Security exposure |

#### High-Priority Issues
| # | Issue | File | Impact |
|---|---|---|---|
| 5 | **God class** — 905 lines, 14 cached views, navigation+cache+sidebar+animation | `MainView.java` | Unmaintainable |
| 6 | **No input validation on login** | `LoginController.java` L93-94 | NPE risk |
| 7 | **Rate limiting bypass** on SQL failure (returns false instead of throwing) | `AuthService.java` L101-105 | Security gap |
| 8 | **ForgotPasswordController has inline SQL** | `ForgotPasswordController.java` | MVC violation |
| 9 | **Raw `new Thread()` everywhere** (~20+ instances) | Multiple files | Thread leaks, no shutdown |
| 10 | **ProcessBuilder runs Python with no timeout/sandboxing** | `JobService.java` L345 | Hang/security risk |

#### Medium-Priority Issues
| # | Issue | Impact |
|---|---|---|
| 11 | Hardcoded dashboard/report data (not from DB) | Misleading UI |
| 12 | `System.err.println` / `e.printStackTrace()` instead of SLF4J (~20+ instances) | Poor observability |
| 13 | Empty catch blocks (`catch (Exception ignored) {}`) — 7+ instances | Silent failures |
| 14 | No DAO/Repository layer — SQL embedded in services | 542-line JobService |
| 15 | Singleton overuse (10+ classes) — no DI | Untestable |
| 16 | View caching inconsistency | Memory/perf unpredictable |
| 17 | Inner class `Report` with all public fields | No encapsulation |

#### Positive Patterns (Keep)
- ✅ All SQL uses `PreparedStatement` (no injection)
- ✅ BCrypt with cost 12 for passwords
- ✅ HikariCP properly configured
- ✅ Rate limiting on login (5 attempts / 15 min)
- ✅ `Platform.runLater()` used correctly for FX thread safety
- ✅ `Validators` utility class exists
- ✅ FXML migration mostly complete (21 views)
- ✅ SLF4J used in service layer

### 2.3 Testing & Documentation Status

| Aspect | Status |
|---|---|
| Unit tests | ❌ **None** — no `src/test/` directory exists |
| Test framework in pom.xml | ❌ **Missing** — no JUnit 5, Mockito, or TestFX |
| Test coverage | **0%** |
| README.md | ✅ Good — comprehensive project description |
| PROJECT_STRUCTURE.md | ✅ Good — architecture documentation |
| Inline Javadoc | ❌ Minimal — most classes have no Javadoc |
| API documentation | ❌ None |
| SQL schema docs | ⚠️ `skilora.sql` exists but no migration strategy |

### 2.4 Performance & Optimization

| Area | Finding |
|---|---|
| **Connection pooling** | ✅ HikariCP properly sized |
| **View caching** | ⚠️ Inconsistent — 5 views cached, 9 recreated each time |
| **Image/Media caching** | ✅ `ImageCache` and `MediaCache` services exist |
| **Thread management** | ❌ Raw `new Thread()` — no `ExecutorService`, threads not daemon |
| **GPU forcing** | ⚠️ `System.setProperty("prism.forceGPU", "true")` — may crash on non-GPU systems |
| **Camera startup** | ✅ Uses `webcam-capture` (1-3s) instead of slow JavaCV |
| **Heavy dependency** | ⚠️ `javacv-platform` (~500MB) loaded even if not needed |
| **Unused code** | ⚠️ Several unused imports across controllers |

### 2.5 Architecture & Design

```
Current Architecture:                    Target Architecture:
┌──────────────┐                        ┌──────────────┐
│  FXML Views  │                        │  FXML Views  │
├──────────────┤                        ├──────────────┤
│ Controllers  │ ← direct getInstance   │ Controllers  │ ← injected
├──────────────┤                        ├──────────────┤
│   Services   │ ← SQL embedded         │   Services   │ ← business logic only
├──────────────┤                        ├──────────────┤
│  (missing)   │                        │     DAOs     │ ← SQL isolated
├──────────────┤                        ├──────────────┤
│    MySQL     │                        │    MySQL     │
└──────────────┘                        └──────────────┘
```

**Key architecture gaps:**
1. No DAO layer → SQL mixed into 542-line services
2. No DI → every class tightly coupled via `getInstance()`
3. MainView is a monolith → should split into NavigationManager + SidebarComponent
4. No event bus → controllers can't communicate cleanly
5. No error handling strategy → mix of silent fails, `System.err`, and proper logging

---

## 3. Six-Week Improvement Roadmap

### Phase 1: Foundation & Quick Wins (Week 1)
> Goal: Fix critical security issues, establish testing infrastructure, clean up obvious debt

| # | Task | Priority | Est. Hours |
|---|---|---|---|
| 1.1 | Remove plaintext password fallback in `UserService.verifyPassword()` | 🔴 Critical | 1h |
| 1.2 | Fix resource leak in `JobService.getOrCreateCompanyByName()` | 🔴 Critical | 1h |
| 1.3 | Move DB credentials to fail-fast env vars (remove hardcoded defaults) | 🔴 Critical | 2h |
| 1.4 | Add JUnit 5 + Mockito + TestFX to `pom.xml`, create `src/test/` structure | 🔴 Critical | 2h |
| 1.5 | Add input validation to `LoginController.handleLogin()` | 🟠 High | 1h |
| 1.6 | Fix rate-limiting bypass in `AuthService.isLockedOut()` | 🟠 High | 1h |
| 1.7 | Replace all `System.err.println`/`printStackTrace` with SLF4J | 🟡 Medium | 3h |
| 1.8 | Remove unused imports across all controllers | 🟢 Low | 1h |
| 1.9 | Add Javadoc to all entity classes | 🟢 Low | 2h |
| 1.10 | Set up Checkstyle/SpotBugs Maven plugins | 🟡 Medium | 2h |

### Phase 2: Code Quality & Refactoring (Week 2-3)
> Goal: Break apart god classes, introduce DAO layer, fix threading

| # | Task | Priority | Est. Hours |
|---|---|---|---|
| 2.1 | Split `MainView.java` into `NavigationManager` + `SidebarComponent` + `ViewCache` | 🟠 High | 8h |
| 2.2 | Extract generic `loadView()` method to eliminate 14x duplicated FXML loading | 🟠 High | 3h |
| 2.3 | Create DAO layer: `UserDAO`, `JobDAO`, `ProfileDAO`, `ApplicationDAO` | 🟠 High | 8h |
| 2.4 | Move SQL from `ForgotPasswordController` into `AuthService`/`AuthDAO` | 🟠 High | 2h |
| 2.5 | Replace all raw `new Thread()` with `ExecutorService` + `javafx.concurrent.Task` | 🟠 High | 4h |
| 2.6 | Encapsulate `Report` inner class — add proper fields, getters, builder | 🟡 Medium | 1h |
| 2.7 | Unify view caching strategy (cache all or use LRU) | 🟡 Medium | 3h |
| 2.8 | Replace `Map<String, Object>` returns with typed DTOs | 🟡 Medium | 3h |
| 2.9 | Fix all empty catch blocks — either log meaningful message or rethrow | 🟡 Medium | 2h |
| 2.10 | Add `ProcessBuilder` timeout + output capture for Python subprocess | 🟠 High | 2h |

### Phase 3: Testing & Reliability (Week 3-4)
> Goal: Achieve baseline test coverage for critical paths

| # | Task | Priority | Est. Hours |
|---|---|---|---|
| 3.1 | Unit tests for `AuthService` (login, lockout, BCrypt) | 🔴 Critical | 4h |
| 3.2 | Unit tests for `UserService` (CRUD, validation, password verify) | 🔴 Critical | 4h |
| 3.3 | Unit tests for `JobService` (CRUD, feed parsing, matching) | 🟠 High | 4h |
| 3.4 | Unit tests for `ProfileService` (CRUD, validation) | 🟠 High | 3h |
| 3.5 | Unit tests for `Validators` utility class | 🟡 Medium | 2h |
| 3.6 | Unit tests for `MatchingService` scoring algorithm | 🟠 High | 3h |
| 3.7 | Integration tests for `DatabaseInitializer` schema setup | 🟡 Medium | 3h |
| 3.8 | TestFX tests for `LoginController` (valid/invalid login flows) | 🟡 Medium | 4h |
| 3.9 | Add Maven CI profile for test execution | 🟡 Medium | 1h |
| 3.10 | Set up JaCoCo for coverage reporting | 🟡 Medium | 2h |

### Phase 4: Performance & Optimization (Week 4-5)
> Goal: Optimize startup, memory, and responsiveness

| # | Task | Priority | Est. Hours |
|---|---|---|---|
| 4.1 | Lazy-load `javacv-platform` — only init when biometrics needed | 🟠 High | 3h |
| 4.2 | Implement `ExecutorService` thread pool (replace 20+ raw threads) | 🟠 High | 4h |
| 4.3 | Make all background threads daemon | 🟡 Medium | 1h |
| 4.4 | Unify view caching with LRU eviction (cap at 5 cached views) | 🟡 Medium | 3h |
| 4.5 | Remove `prism.forceGPU=true` or make it configurable | 🟢 Low | 0.5h |
| 4.6 | Profile startup time and optimize DB init sequence | 🟡 Medium | 3h |
| 4.7 | Add virtual scrolling to job feed (already have `TLVirtualList`) | 🟡 Medium | 3h |
| 4.8 | Optimize CSS — consolidate 3 CSS files, remove unused rules | 🟢 Low | 2h |
| 4.9 | Add connection pool metrics logging | 🟢 Low | 1h |
| 4.10 | Benchmark and optimize `MatchingService` scoring | 🟡 Medium | 3h |

### Phase 5: Advanced Features & Polish (Week 5-6)
> Goal: Connect hardcoded views to real data, error handling, monitoring

| # | Task | Priority | Est. Hours |
|---|---|---|---|
| 5.1 | Replace hardcoded dashboard statistics with real DB queries | 🟠 High | 4h |
| 5.2 | Replace hardcoded reports data with real DB queries | 🟠 High | 3h |
| 5.3 | Implement global error handler (`Thread.setUncaughtExceptionHandler`) | 🟠 High | 2h |
| 5.4 | Add application-level event bus for controller communication | 🟡 Medium | 4h |
| 5.5 | Create user-facing error dialogs for all silent failures | 🟡 Medium | 3h |
| 5.6 | Add startup health check (DB connectivity, Python availability) | 🟡 Medium | 2h |
| 5.7 | Implement config file migration strategy (version schema changes) | 🟡 Medium | 3h |
| 5.8 | Complete Javadoc coverage for all public APIs | 🟢 Low | 4h |
| 5.9 | Create developer onboarding guide | 🟢 Low | 2h |
| 5.10 | Add logging dashboard/log file rotation | 🟢 Low | 2h |

---

## 4. Agent Task Matrix

### How VS Code Multi-Agent System Works for This Project

| Agent Type | Best For | How to Invoke |
|---|---|---|
| **Local Agent** | Interactive decisions, debugging, architecture discussions, reviewing changes | Default chat — just type in Copilot Chat |
| **Background Agent** | Autonomous well-defined tasks (refactoring, test writing, docs) — runs in isolated git worktree | Click "Continue In" → Background, or use `@cli` |
| **Cloud Agent** | PR reviews, CI-triggered tasks, team collaboration | Click "Continue In" → Cloud |
| **Custom Agent** | Recurring specialized roles you define | Cmd+Shift+P → "Chat: New Custom Agent" |

### Task Assignment Matrix

| Task | Agent | Rationale |
|---|---|---|
| **1.1** Remove plaintext password fallback | 🖥️ LOCAL | Needs security decision + verification |
| **1.2** Fix JobService resource leak | 🔄 BACKGROUND | Well-defined, isolated fix |
| **1.3** Externalize DB credentials | 🖥️ LOCAL | Needs env var naming decisions |
| **1.4** Add test infrastructure to pom.xml | 🔄 BACKGROUND | Mechanical — add deps + create dirs |
| **1.5** Login validation | 🔄 BACKGROUND | Clear requirements |
| **1.6** Fix rate-limiting bypass | 🖥️ LOCAL | Security implications to discuss |
| **1.7** Replace System.err with SLF4J | 🔄 BACKGROUND | Repetitive, well-defined |
| **1.8** Remove unused imports | 🔄 BACKGROUND | Fully automated |
| **1.9** Add entity Javadoc | 🔄 BACKGROUND | Autonomous doc generation |
| **1.10** Set up Checkstyle/SpotBugs | 🔄 BACKGROUND | Config-based, no decisions |
| **2.1** Split MainView | 🖥️ LOCAL | Complex refactor, needs real-time feedback |
| **2.2** Extract generic loadView() | 🖥️ LOCAL | Architectural decision |
| **2.3** Create DAO layer | 🔄 BACKGROUND | Systematic extraction |
| **2.4** Move SQL from ForgotPasswordController | 🔄 BACKGROUND | Straightforward extraction |
| **2.5** Replace raw threads with ExecutorService | 🖥️ LOCAL | Needs thread lifecycle decisions |
| **2.6** Encapsulate Report class | 🔄 BACKGROUND | Simple refactor |
| **2.7-2.10** Remaining Phase 2 | 🔄 BACKGROUND | Systematic, well-defined |
| **3.1-3.6** Service unit tests | 🔄 BACKGROUND | Test writing is well-suited for agents |
| **3.7-3.8** Integration/UI tests | 🖥️ LOCAL | Needs runtime feedback |
| **3.9-3.10** CI setup | ☁️ CLOUD | Benefits from PR + CI validation |
| **4.1-4.10** Performance tasks | Mixed | See parallel plan below |
| **5.1-5.2** Real data integration | 🖥️ LOCAL | Needs DB schema understanding |
| **5.3-5.10** Polish tasks | 🔄 BACKGROUND | Most are well-defined |

---

## 5. Parallel Execution Plan

### Week 1 — Three Parallel Workstreams

```
Workstream A (YOU + Local Agent):          Workstream B (Background Agent #1):     Workstream C (Background Agent #2):
─────────────────────────────────          ─────────────────────────────────        ─────────────────────────────────
Day 1: 1.1 Remove plaintext pwd           Day 1: 1.4 Add test infrastructure      Day 1: 1.8 Remove unused imports
Day 1: 1.3 Externalize DB creds           Day 2: 1.5 Login validation             Day 1: 1.9 Add entity Javadoc
Day 2: 1.6 Fix rate-limiting              Day 2: 1.7 Replace System.err→SLF4J     Day 2: 1.10 Setup Checkstyle
Day 3: Review all background results      Day 3: 1.2 Fix JobService resource leak  Day 3: (idle — review)
```

### Week 2-3 — Four Parallel Workstreams

```
Workstream A (Local Agent):               Workstream B (Background #1):           
─────────────────────────────              ─────────────────────────────           
2.1 Split MainView (3 days)               2.3 Create DAO layer (3 days)           
2.2 Extract generic loadView()            2.4 Move ForgotPwd SQL                  
2.5 Replace raw threads                   2.6 Encapsulate Report class            
                                          2.8 DTOs for Map returns                

Workstream C (Background #2):             Workstream D (Background #3):
─────────────────────────────              ─────────────────────────────
3.1 AuthService tests                     3.3 JobService tests
3.2 UserService tests                     3.4 ProfileService tests
3.5 Validators tests                      3.6 MatchingService tests
```

### Week 4-5 — Three Streams

```
Workstream A (Local Agent):               Workstream B (Background #1):            Workstream C (Cloud Agent):
─────────────────────────────              ─────────────────────────────             ─────────────────────────────
4.2 ExecutorService pool                  4.1 Lazy-load javacv                     3.9 Maven CI profile (PR)
4.6 Startup profiling                     4.3 Daemon threads                       3.10 JaCoCo coverage (PR)
4.7 Virtual scrolling                     4.4 LRU view cache                       Submit Phase 1-2 for review
4.10 MatchingService bench                4.8 CSS consolidation
```

### Week 5-6 — Final Sprint

```
Workstream A (Local Agent):               Workstream B (Background):               Workstream C (Cloud):
─────────────────────────────              ─────────────────────────────             ─────────────────────────────
5.1 Real dashboard data                   5.3 Global error handler                 Final PR with all improvements
5.2 Real reports data                     5.4 Event bus                            CI validation
5.6 Startup health check                  5.5 Error dialogs                        Coverage report review
                                          5.8 Complete Javadoc
                                          5.9 Developer guide
```

---

## 6. Hand-off Workflows

### Workflow A: Security Fix (Phase 1)

```
1. LOCAL: Analyze security issue, decide on fix approach
   ↓ (confirmed approach)
2. LOCAL: Implement fix with your guidance (needs realtime decisions)
   ↓ (fix verified locally)
3. CLOUD: Submit PR for team security review
   ↓ (approved + merged)
4. LOCAL: Verify fix in integrated build
```

### Workflow B: Refactoring (Phase 2)

```
1. LOCAL: Design target architecture (e.g., DAO interface signatures)
   ↓ (interface contracts defined)
2. BACKGROUND: Implement DAO classes by extracting SQL from services
   ↓ (agent completes in git worktree)
3. LOCAL: Review changes, resolve any conflicts
   ↓ (changes approved)
4. BACKGROUND: Write unit tests for new DAOs
   ↓ (tests pass)
5. CLOUD: Submit PR with both implementation + tests
```

### Workflow C: Test Writing (Phase 3)

```
1. LOCAL: Define test strategy — which methods to test, edge cases
   ↓ (test plan documented)
2. BACKGROUND: Write all test classes autonomously
   ↓ (tests written in worktree)
3. LOCAL: Run tests, fix failures, adjust mocks
   ↓ (all green)
4. CLOUD: Submit coverage improvement PR
```

### Workflow D: Performance (Phase 4)

```
1. LOCAL: Profile app, identify bottleneck
   ↓ (data collected)
2. BACKGROUND: Implement optimization (e.g., thread pool, lazy loading)
   ↓ (implemented in worktree)
3. LOCAL: Benchmark before/after, validate improvement
   ↓ (improvement confirmed)
4. CLOUD: Submit performance PR with benchmark data
```

---

## 7. Custom Agent Specifications

### Agent 1: "Skilora Code Reviewer"

**Create via:** `Cmd+Shift+P` → `Chat: New Custom Agent`

```
Name: skilora-reviewer
Description: Security and quality reviewer for Skilora JavaFX project

Instructions:
You are a code reviewer for a JavaFX 21 / Java 17 desktop application using MVC architecture 
with MySQL. Focus on:

1. SECURITY (highest priority):
   - SQL injection (all queries must use PreparedStatement)
   - Password handling (BCrypt only, no plaintext)
   - Input validation on all controller methods
   - Resource leaks (PreparedStatement, ResultSet, Connection must be in try-with-resources)

2. THREAD SAFETY:
   - All UI updates must use Platform.runLater()
   - No raw new Thread() — use ExecutorService or javafx.concurrent.Task
   - All threads should be daemon threads

3. ARCHITECTURE:
   - Controllers should NOT contain SQL
   - Services should delegate to DAO classes
   - No getInstance() in controllers — use constructor injection
   - Methods should be <50 lines

4. ERROR HANDLING:
   - No empty catch blocks
   - No System.err.println or e.printStackTrace()
   - Use SLF4J Logger
   - All service methods should either return Optional or throw checked exceptions

Output a structured review with: CRITICAL, HIGH, MEDIUM, LOW findings.

Tools: Read-only workspace access
```

### Agent 2: "Skilora Test Writer"

```
Name: skilora-test-writer
Description: JUnit 5 test generator for Skilora services

Instructions:
You write JUnit 5 + Mockito tests for a JavaFX desktop app with MySQL backend.

Conventions:
- Test class: src/test/java/com/skilora/model/service/{ServiceName}Test.java
- Use @ExtendWith(MockitoExtension.class)
- Mock all database dependencies (Connection, PreparedStatement, ResultSet)
- Use @Mock for dependencies, @InjectMocks for the service under test
- Test naming: methodName_condition_expectedResult()
- Every test method should have: // Arrange, // Act, // Assert sections
- Cover: happy path, null inputs, empty results, SQL exceptions, edge cases
- Minimum 5 tests per service method

For service classes that use DatabaseConfig.getInstance():
- Mock the static getInstance() using Mockito's mockStatic()
- Verify connection is closed after use
- Test both success and SQLException paths

Tools: Read + Write access
```

### Agent 3: "Skilora Doc Writer"

```
Name: skilora-docs
Description: Documentation generator for Skilora codebase

Instructions:
You generate comprehensive Javadoc and markdown documentation for a JavaFX project.

Conventions:
- Javadoc: Every public class, method, and field
- Use @param, @return, @throws, @since 1.0.0
- Include code examples for complex methods
- For entity classes: document all fields and their DB column mappings
- For services: document thread safety guarantees
- For controllers: document which FXML bindings they expect

Markdown docs:
- Use tables for structured data
- Include architecture diagrams (Mermaid syntax)
- Keep language professional but accessible
- Include "Getting Started" sections

Tools: Read + Write access
```

### Agent 4: "Skilora DAO Extractor"

```
Name: skilora-dao
Description: Extracts SQL from services into DAO classes

Instructions:
You extract data access code from service classes into dedicated DAO classes.

Pattern to follow:
1. Create interface: com.skilora.model.dao.{Entity}DAO
2. Create implementation: com.skilora.model.dao.impl.{Entity}DAOImpl
3. Move all SQL queries from {Entity}Service to {Entity}DAOImpl
4. Service should only have business logic, calling DAO methods
5. DAO methods use try-with-resources for ALL JDBC resources
6. DAO returns Optional<T> for single-entity queries, List<T> for multi
7. DAO throws DAOException (custom) wrapping SQLException

Naming conventions:
- findById(int id) → Optional<T>
- findAll() → List<T>
- save(T entity) → T (with generated ID)
- update(T entity) → boolean
- delete(int id) → boolean
- findByXxx(xxx) → List<T> or Optional<T>

Tools: Read + Write access
```

---

## 8. Success Metrics Dashboard

| Metric | Current (Week 0) | Target (Week 6) | How to Measure |
|---|---|---|---|
| **Test Coverage** | 0% | 60%+ | JaCoCo report (`mvn verify`) |
| **Critical Security Issues** | 4 | 0 | Manual audit + SpotBugs |
| **High Security Issues** | 3 | 0 | Manual audit |
| **God Classes (>500 LOC)** | 1 (MainView: 905) | 0 | `wc -l` on all .java files |
| **Raw `new Thread()` Usage** | ~20 | 0 | `grep -r "new Thread"` |
| **Empty Catch Blocks** | 7+ | 0 | `grep -r "ignored"` |
| **`System.err` / `printStackTrace`** | 20+ | 0 | `grep -r "System.err\|printStackTrace"` |
| **Controllers with SQL** | 1 | 0 | Manual review |
| **Services without DAO** | 7 | 0 | Architecture review |
| **Javadoc Coverage** | ~5% | 80%+ | Checkstyle report |
| **SpotBugs Warnings** | Unknown | <10 | SpotBugs Maven report |
| **Startup Time** | Unknown | <3s | Manual timing |
| **Hardcoded Data Views** | 2 (Dashboard, Reports) | 0 | Code review |

---

## 9. Daily Workflow Guide

### Morning Routine (30 min)

```
□ Check overnight Background Agent results
   → VS Code: Chat view → Sessions → Review completed sessions
   → Accept or reject changes from git worktree merges

□ Check Cloud Agent PR status (if any)
   → GitHub/GitLab → Review CI results

□ Plan today's tasks
   → Open this file → check which Phase/Task is next
   → Decide: LOCAL interactive work vs. BACKGROUND delegation
```

### Work Session 1: Morning (2-3 hours)

```
□ LOCAL Agent: Work on current interactive task
   → Open Copilot Chat (Ctrl+Shift+I)
   → Discuss approach, make decisions, implement with feedback

□ Kick off Background Agents for independent tasks
   → Type task description in chat
   → Click "Continue In" → Background
   → Agent works autonomously in git worktree
   
□ Repeat for up to 3 parallel background tasks
```

### Midday Check (15 min)

```
□ Review any Background Agent completions
   → Sessions view → check for finished tasks
   → Review diff, merge if good

□ Quick commit of morning's local work
   → git add . && git commit -m "Phase X.Y: description"
```

### Work Session 2: Afternoon (2-3 hours)

```
□ Continue LOCAL work on complex tasks
□ Monitor Background Agent progress
□ When ready: Submit completed work to Cloud Agent for PR

□ Cloud Agent submission:
   → Click "Continue In" → Cloud
   → Or: manually create PR with description template
```

### End of Day (15 min)

```
□ Review all agent session results
□ Commit any uncommitted local changes
□ Archive completed chat sessions
□ Update this plan — mark completed tasks with ✅
□ Write tomorrow's task assignments in a quick note
```

### Weekly Review (Friday, 1 hour)

```
□ Run metrics check:
   mvn verify                         # Tests pass?
   mvn spotbugs:check                 # Static analysis
   mvn jacoco:report                  # Coverage %

□ Compare metrics to targets in Section 8
□ Adjust next week's plan based on velocity
□ Refine custom agent instructions based on quality of output
□ Clean up stale git worktrees:
   git worktree list
   git worktree remove <path>
```

---

## 10. Quick Start Commands

### Immediate Setup (Do This Now)

```powershell
# 1. Create test directory structure
mkdir -p src/test/java/com/skilora/model/service
mkdir -p src/test/java/com/skilora/model/dao
mkdir -p src/test/java/com/skilora/controller
mkdir -p src/test/resources

# 2. Verify project builds
mvn clean compile

# 3. Check current thread count and issues
grep -rn "new Thread" src/main/java/ | wc -l
grep -rn "System.err\|printStackTrace" src/main/java/ | wc -l
grep -rn "catch.*ignored\|catch.*Exception e)" src/main/java/ | wc -l
```

### VS Code Multi-Agent Commands

| Action | Command |
|---|---|
| Open Copilot Chat | `Ctrl+Shift+I` |
| New Chat Editor (dedicated window) | `Ctrl+Shift+P` → `Chat: New Chat Editor` |
| Delegate to Background Agent | In chat → `Continue In` button → `Background` |
| Delegate to Cloud Agent | In chat → `Continue In` button → `Cloud` |
| View all sessions | Chat sidebar → `Show All Sessions` |
| Create Custom Agent | `Ctrl+Shift+P` → `Chat: New Custom Agent` → `Local` |
| Open file from chat | Click any file link in chat response |

### Ready-to-Use Background Agent Prompts

**Prompt 1 — Add Test Infrastructure (Phase 1.4):**
```
CURRENT STATE: pom.xml already has JUnit 5.10.1, Mockito 5.8.0, TestFX 4.0.18 in properties.
src/test/ directory exists but has no Java test files yet.

TASK: Add test dependencies to pom.xml <dependencies> section (NOT just properties):
- org.junit.jupiter:junit-jupiter (version ${junit.version})
- org.mockito:mockito-core (version ${mockito.version})
- org.mockito:mockito-junit-jupiter (version ${mockito.version})
- org.testfx:testfx-core (version ${testfx.version})
- org.testfx:testfx-junit5 (version ${testfx.version})

Create directory structure:
- src/test/java/com/skilora/model/service/
- src/test/java/com/skilora/model/dao/
- src/test/java/com/skilora/controller/
- src/test/java/com/skilora/utils/
- src/test/resources/

Create sample test: src/test/java/com/skilora/model/service/AuthServiceTest.java
- @ExtendWith(MockitoExtension.class)
- Mock DatabaseConfig.getInstance() using Mockito.mockStatic()
- Mock Connection, PreparedStatement, ResultSet
- Test method: testLoginSuccessful() - verify BCrypt password check
- Test method: testLoginFailure() - verify null return for wrong password

Add JaCoCo Maven plugin to pom.xml:
- Goal: jacoco-maven-plugin:0.8.11
- Executions: prepare-agent, report
- Output: target/site/jacoco/index.html
```

**Prompt 2 — Replace System.err with SLF4J (Phase 1.7):**
```
CURRENT STATE: grep shows System.err/printStackTrace ALREADY REMOVED from most files. 
Only 0 System.err instances found. Good! This task is COMPLETE ✅

VERIFICATION TASK INSTEAD: Double-check remaining files have proper SLF4J logging:
Files with "new Thread()" that may need logging improvements:
- MediaCache.java (1 instance)
- JobService.java (2 instances)
- Main.java (2 instances)
- UsersController.java (1 instance)
- RegisterController.java (1 instance)
- ProfileWizardController.java (2 instances)
- PostJobController.java (3 instances)
- MyOffersController.java (3 instances)
- LoginController.java (2 instances)
- InterviewsController.java (2 instances)
- ForgotPasswordController.java (2 instances)
- FeedController.java (1 instance)
- BiometricAuthController.java (2 instances)
- ApplicationsController.java (1 instance)
- ApplicationInboxController.java (3 instances)
- ActiveOffersController.java (3 instances)

For each file, verify:
1. Has `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
2. All catch blocks use logger.error("message", e) NOT System.err or printStackTrace()
3. Background thread exceptions are logged (not silent)
4. Add logger imports if missing: import org.slf4j.Logger; import org.slf4j.LoggerFactory;
```

**Prompt 3 — Create DAO Layer (Phase 2.3):**
```
CURRENT STATE: No DAO layer exists. All services (AuthService, UserService, JobService, ProfileService, ApplicationService, BiometricService, MatchingService) contain SQL queries directly.

TASK: Create comprehensive DAO layer following this pattern:

Step 1 - Create DAOException:
File: src/main/java/com/skilora/model/dao/DAOException.java
- Extends RuntimeException
- Constructor: DAOException(String message, Throwable cause)
- Wraps SQLException from JDBC operations

Step 2 - Create UserDAO interface and implementation:
Interface: src/main/java/com/skilora/model/dao/UserDAO.java
Methods needed (extract from UserService.java):
- Optional<User> findById(int id)
- Optional<User> findByUsername(String username)
- Optional<User> findByEmail(String email)
- List<User> findAll()
- User save(User user)  // returns User with generated ID
- boolean update(User user)
- boolean delete(int id)
- boolean existsByUsername(String username)
- boolean existsByEmail(String email)

Implementation: src/main/java/com/skilora/model/dao/impl/UserDAOImpl.java
- All SQL queries from UserService lines 69-250
- Use try-with-resources for Connection, PreparedStatement, ResultSet
- Throw DAOException wrapping SQLException
- Use DatabaseConfig.getInstance().getConnection()

Step 3 - Update UserService.java:
- Add field: `private final UserDAO userDAO = new UserDAOImpl();`
- Replace all direct SQL with userDAO method calls
- Remove all PreparedStatement/ResultSet code
- Keep business logic (password verification, validation)

Step 4 - Repeat for other services:
Create JobDAO (extract from JobService.java ~542 lines):
- findById, findAll, findByEmployer, findByStatus, save, update, delete, search

Create ProfileDAO (extract from ProfileService.java):
- findByUserId, save, update, delete, findBySkills

Create ApplicationDAO (extract from ApplicationService.java):
- findById, findByJobId, findByApplicantId, save, updateStatus, delete

Create BiometricDAO (extract from BiometricService.java):
- saveEncoding, getEncoding, updateEncoding, deleteEncoding

Step 5 - Verify:
Run `mvn clean compile` to ensure all changes compile successfully.
```

**Prompt 4 — Write Unit Tests for AuthService (Phase 3.1):**
```
CURRENT STATE: No test files exist. Test infrastructure needs to be added first (see Prompt 1).
PREREQUISITE: Complete Prompt 1 before starting this task.

FILE LOCATION: src/main/java/com/skilora/model/service/AuthService.java

TASK: Create src/test/java/com/skilora/model/service/AuthServiceTest.java

Test class structure:
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;
    @InjectMocks private AuthService authService;
    
    private MockedStatic<DatabaseConfig> databaseConfigMock;
    private DatabaseConfig dbConfig;
    
    @BeforeEach
    void setUp() {
        // Mock DatabaseConfig singleton
        databaseConfigMock = Mockito.mockStatic(DatabaseConfig.class);
        dbConfig = Mockito.mock(DatabaseConfig.class);
        databaseConfigMock.when(DatabaseConfig::getInstance).thenReturn(dbConfig);
    }
    
    @AfterEach
    void tearDown() {
        databaseConfigMock.close();
    }
}
```

Test methods to implement:
1. testLoginSuccessful_WithValidCredentials()
   - Mock: User exists in DB, BCrypt password matches
   - Verify: Returns User object with correct details
   
2. testLoginFailure_WithWrongPassword()
   - Mock: User exists, password does NOT match
   - Verify: Returns null
   
3. testLoginFailure_WithNonExistentUser()
   - Mock: ResultSet.next() returns false (no user found)
   - Verify: Returns null
   
4. testLoginLockedOut_AfterFiveFailedAttempts()
   - Mock: 5 failed login attempts within 15 minutes
   - Verify: isLockedOut() returns true, login returns null
   
5. testLockoutExpires_AfterFifteenMinutes()
   - Mock: Lockout timestamp is 16 minutes ago
   - Verify: isLockedOut() returns false, login allowed
   
6. testLoginValidation_NullUsername()
   - Verify: Returns null without DB query
   
7. testLoginValidation_EmptyPassword()
   - Verify: Returns null without DB query
   
8. testLoginHandlesSQLException()
   - Mock: connection.prepareStatement() throws SQLException
   - Verify: Method catches exception, logs error, returns null

Mock setup example:
```java
when(dbConfig.getConnection()).thenReturn(connection);
when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
when(preparedStatement.executeQuery()).thenReturn(resultSet);
when(resultSet.next()).thenReturn(true);
when(resultSet.getInt("id")).thenReturn(1);
when(resultSet.getString("username")).thenReturn("testuser");
when(resultSet.getString("password")).thenReturn(BCrypt.hashpw("password123", BCrypt.gensalt()));
```

Run tests: `mvn test -Dtest=AuthServiceTest`
```

---

## Appendix A: File Impact Map

Files touched per phase (for conflict avoidance when running parallel agents):

| Phase | Files Modified | Conflict Risk |
|---|---|---|
| 1.1 | `UserService.java` | Low |
| 1.2 | `JobService.java` | Low |
| 1.3 | `DatabaseConfig.java` | Low |
| 1.4 | `pom.xml`, new test files | None |
| 1.5 | `LoginController.java` | Low |
| 1.6 | `AuthService.java` | Low (if 1.1 done first) |
| 1.7 | ~15 files (all with System.err) | **Medium** — coordinate with other agents |
| 2.1 | `MainView.java` → 3 new files | **High** — do alone |
| 2.3 | All services → new DAO files | **High** — do alone or after 2.1 |
| 2.5 | ~10 files with `new Thread()` | **Medium** |
| 3.x | New test files only | None (additive) |
| 4.x | Scattered changes | Low |
| 5.x | Controllers + new service methods | Medium |

---

## Appendix B: Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     SKILORA TUNISIA                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐  ┌──────────────────────────────────────┐     │
│  │  FXML    │  │         Controllers (21)              │     │
│  │  Views   │←→│  Login, Dashboard, Feed, Reports...   │     │
│  │  (21)    │  │  Profile, Settings, Users, Jobs...    │     │
│  └──────────┘  └──────────────┬───────────────────────┘     │
│                               │                             │
│  ┌──────────┐  ┌──────────────▼───────────────────────┐     │
│  │   TL*    │  │         Services (7)                  │     │
│  │  Custom  │  │  Auth, User, Job, Profile,            │     │
│  │  UI (47) │  │  Matching, Biometric, Application     │     │
│  └──────────┘  └──────────────┬───────────────────────┘     │
│                               │ (currently direct SQL)      │
│                ┌──────────────▼───────────────────────┐     │
│                │          MySQL + HikariCP             │     │
│                │     (DatabaseConfig singleton)        │     │
│                └──────────────────────────────────────┘     │
│                                                             │
│  ┌──────────────────────┐  ┌────────────────────────┐       │
│  │  Python Subprocess   │  │  Biometric Data        │       │
│  │  (face recognition)  │  │  (encodings.json)      │       │
│  └──────────────────────┘  └────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

**Next Step:** Open Copilot Chat and start with Phase 1.1 (remove plaintext password fallback). Then kick off Background Agents for tasks 1.4, 1.7, and 1.8 in parallel.
