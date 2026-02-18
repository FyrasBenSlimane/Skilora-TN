# Skilora Tunisia — UI/UX Master Improvement Plan

> Every screen, every role, every interaction — polished to production quality.
> **Rule: No emojis.** All icons are SVG paths or icon font glyphs.

---

## Table of Contents

1. [Global Design System](#1-global-design-system)
2. [Auth Module](#2-auth-module)
3. [Dashboard](#3-dashboard)
4. [User Management (Admin)](#4-user-management-admin)
5. [Recruitment Module](#5-recruitment-module)
6. [Formation Module](#6-formation-module)
7. [Finance Module](#7-finance-module)
8. [Support Module](#8-support-module)
9. [Community Module](#9-community-module)
10. [Settings & Profile](#10-settings--profile)
11. [Cross-Cutting UX](#11-cross-cutting-ux)

---

## 1. Global Design System

### 1.1 Icon System
| ID | Task | Priority | Status |
|----|------|----------|--------|
| G-01 | Replace ALL emoji characters with SVG icons across the entire app (~80 occurrences in Java + FXML) | HIGH | DONE |
| G-02 | Create `SvgIcons` utility class with ~35 static SVG path constants (Lucide-style, 24×24 viewBox) + factory methods | HIGH | DONE |
| G-03 | Built `icon()`, `filledIcon()`, `withText()` factory methods in SvgIcons utility | HIGH | DONE |

### 1.2 Typography & Spacing
| ID | Task | Priority | Status |
|----|------|----------|--------|
| G-04 | Audit all hardcoded French strings in FXML — move to i18n `messages_{locale}.properties` | MEDIUM | TODO |
| G-05 | Define reusable CSS classes for common inline font overrides (`.text-11`, `.text-13`, `.font-bold`) | LOW | TODO |

### 1.3 Animations & Transitions
| ID | Task | Priority | Status |
|----|------|----------|--------|
| G-06 | Add exit animation (fade-out 150ms) before view swap in `MainView.showView()` | MEDIUM | TODO |
| G-07 | Add staggered card entry animation for grid/list views (feed, offers, applications) | LOW | TODO |
| G-08 | Add hover scale microinteraction on cards (1.0 → 1.02, 120ms ease) | LOW | TODO |

### 1.4 Loading & Empty States
| ID | Task | Priority | Status |
|----|------|----------|--------|
| G-09 | Standardize loading state: spinner + label in a centered VBox, reusable across all views | MEDIUM | TODO |
| G-10 | Add skeleton shimmer loading for card grids (feed, offers, formations) | LOW | TODO |
| G-11 | Standardize empty state design: icon + title + subtitle + optional action button | MEDIUM | TODO |

---

## 2. Auth Module

### 2.1 Login View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| A-01 | Add show/hide password toggle (eye icon) to `TLPasswordField` component | HIGH | DONE |
| A-02 | Fix "Forgot password?" link alignment — right-aligned, VBox spacing=4 | HIGH | DONE |
| A-03 | Remove biometric reminder emoji (lock icon: use SVG) | HIGH | DONE |
| A-04 | Add focus ring animation on input fields when focused | LOW | TODO |
| A-05 | Add subtle entrance animation for form side (slide-in from right, 400ms) | MEDIUM | TODO |
| A-06 | Hero video: ensure text overlay has consistent gradient regardless of video brightness | LOW | DONE |

### 2.2 Register View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| A-07 | Add show/hide password toggle on both password fields (TLPasswordField rewrite covers all usages) | HIGH | DONE |
| A-08 | Add password strength indicator bar below password field (weak/fair/strong/very strong) | MEDIUM | TODO |
| A-09 | Add real-time validation feedback (green check or red X next to each field as user types) | MEDIUM | TODO |
| A-10 | Add role selector visual: card-based selection instead of dropdown (Job Seeker / Employer / Trainer) | LOW | TODO |

### 2.3 Forgot Password View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| A-11 | Step indicator already themed — verify visual after matte charcoal update | LOW | DONE |
| A-12 | OTP input: replace single text field with 6 individual digit boxes (auto-advance) | MEDIUM | TODO |
| A-13 | Success animation on password reset completion (checkmark circle scale-in) | LOW | TODO |

### 2.4 Biometric Auth Dialog
| ID | Task | Priority | Status |
|----|------|----------|--------|
| A-14 | Viewfinder colors now use theme tokens — verify visual | LOW | DONE |
| A-15 | Add scanning progress feedback bar below viewfinder | LOW | DONE |
| A-16 | Camera preview: ensure dark background stays dark (intentional `#0a0a0a` kept for contrast) — switched to `-fx-background` | LOW | DONE |

---

## 3. Dashboard

### 3.1 Admin Dashboard
| ID | Task | Priority | Status |
|----|------|----------|--------|
| D-01 | Stat cards: add subtle trend arrow icon (up/down SVG) next to delta values | MEDIUM | TODO |
| D-02 | Activity feed: add icon per activity type (login=key, create=plus, update=pencil) instead of plain text | MEDIUM | TODO |
| D-03 | Quick actions: add hover effect (bg lighten + slight scale) | LOW | TODO |
| D-04 | Replace greeting emoji (wave hand) — removed, clean text greeting | HIGH | DONE |

### 3.2 Employer Dashboard
| ID | Task | Priority | Status |
|----|------|----------|--------|
| D-05 | Add "recent applications" summary list below stats | MEDIUM | TODO |
| D-06 | Add "interview today" alert banner if any scheduled today | LOW | TODO |

### 3.3 User Dashboard
| ID | Task | Priority | Status |
|----|------|----------|--------|
| D-07 | Add "profile completion" progress bar with percentage | MEDIUM | TODO |
| D-08 | Add "recommended jobs" preview row (top 3 from feed) | LOW | TODO |

---

## 4. User Management (Admin)

### 4.1 Users List View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| U-01 | Table: add avatar column with initials or profile image | MEDIUM | TODO |
| U-02 | Add bulk actions: select multiple users for enable/disable/delete | LOW | TODO |
| U-03 | Add user detail slide-over panel instead of modal dialog | LOW | TODO |

### 4.2 User Form Dialog
| ID | Task | Priority | Status |
|----|------|----------|--------|
| U-04 | Validation borders now use theme token — verify visual | LOW | DONE |
| U-05 | Add role badge preview next to role dropdown | LOW | TODO |

---

## 5. Recruitment Module

### 5.1 Feed View (User)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-01 | Replace all emoji icons in job cards with SVG (MAP_PIN, BRIEFCASE, DOLLAR_SIGN, CALENDAR) | HIGH | DONE |
| R-02 | Add card hover effect: subtle border highlight + shadow increase | MEDIUM | TODO |
| R-03 | Add "new" badge for jobs posted within 24h | LOW | TODO |
| R-04 | Improve tag filter: styled pills with active state indicator | MEDIUM | TODO |
| R-05 | Add job card save/bookmark icon (heart SVG) with toggle animation | MEDIUM | TODO |

### 5.2 Job Details View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-06 | Replace emoji prefixes (MAP_PIN, BRIEFCASE, CLOCK, BOOKMARK→HEART toggle) with SVG icons | HIGH | DONE |
| R-07 | Benefits section: green check icon already themed — verify | LOW | DONE |
| R-08 | Add "Similar Jobs" section below job description | LOW | TODO |
| R-09 | Add apply confirmation dialog with resume preview | LOW | TODO |

### 5.3 Applications View (User)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-10 | Replace Kanban column emoji headers with SVG icons (FILE_TEXT, EYE, MESSAGE_CIRCLE, SPARKLES) | HIGH | DONE |
| R-11 | Application cards: add company logo placeholder | LOW | TODO |
| R-12 | Add drag hint or status badge color per column | MEDIUM | TODO |

### 5.4 My Offers View (Employer)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-13 | Replace emoji icons with SVGs in offer cards (MAP_PIN, BRIEFCASE, DOLLAR_SIGN, CALENDAR) | HIGH | DONE |
| R-14 | Add offer status badge (active/closed/draft) with colored dot | MEDIUM | TODO |
| R-15 | Add confirmation dialog before delete/close offer | LOW | TODO |

### 5.5 Post Job View (Employer)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-16 | Replace save/publish emoji with SVG icons (SAVE, SEND, ARROW_LEFT, ARROW_RIGHT) | HIGH | DONE |
| R-17 | Step wizard: add animated progress bar between steps | MEDIUM | TODO |
| R-18 | Preview step: make it look like an actual job card preview | LOW | TODO |

### 5.6 Application Inbox (Employer)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-19 | Replace action emoji with SVG icons (SEARCH, REFRESH, ARROW_LEFT/RIGHT) + cleaned context menu | HIGH | DONE |
| R-20 | Add candidate avatar/initials in table rows | MEDIUM | TODO |
| R-21 | Add status filter chips above table | MEDIUM | TODO |

### 5.7 Interviews View (Employer)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-22 | Replace emoji icons with SVGs (BRIEFCASE, MAP_PIN, CALENDAR) | HIGH | DONE |
| R-23 | Calendar view option alongside list view | LOW | TODO |
| R-24 | Add interview countdown timer for upcoming interviews | LOW | TODO |

### 5.8 Active Offers View (Admin)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| R-25 | Add employer name/avatar to offer cards | MEDIUM | TODO |
| R-26 | Add bulk moderation actions (close multiple) | LOW | TODO |

---

## 6. Formation Module

### 6.1 Formations View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-01 | Replace building/school emoji with SVG (GRADUATION_CAP, TIMER) | HIGH | DONE |
| F-02 | Course cards: add progress bar for enrolled courses | MEDIUM | TODO |
| F-03 | Add course detail modal with full description, syllabus, reviews | LOW | TODO |
| F-04 | Category filter: use icon + text chips instead of plain buttons | MEDIUM | TODO |

### 6.2 Mentorship View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| F-05 | Mentorship cards: badges now show title-cased status — verify visual | LOW | DONE |
| F-06 | Add mentor avatar/photo in cards | MEDIUM | TODO |
| F-07 | Add session scheduling UI for active mentorships | LOW | TODO |

---

## 7. Finance Module

### 7.1 Finance View (User)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| FI-01 | Empty state labels now use theme token — verify | LOW | DONE |
| FI-02 | Contract card: add visual status badge (signed/pending/expired) | MEDIUM | TODO |
| FI-03 | Payslip list: add download/export icon | LOW | TODO |
| FI-04 | Salary history: add chart visualization (line or bar) | MEDIUM | TODO |

### 7.2 Finance Admin View (Employer/Admin)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| FI-05 | Contract creation form: add date picker for start/end dates | MEDIUM | TODO |
| FI-06 | Payroll generation: add batch processing indicator | LOW | TODO |
| FI-07 | Reports tab: add exportable charts for revenue/expenses | LOW | TODO |

---

## 8. Support Module

### 8.1 Support View (User/Employer)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| S-01 | Chatbot bubbles now use theme tokens — verify visual | LOW | DONE |
| S-02 | Ticket list: add unread indicator dot | MEDIUM | TODO |
| S-03 | Chatbot: add typing indicator animation (3 bouncing dots) | MEDIUM | TODO |
| S-04 | FAQ: add search/filter within FAQ section | LOW | TODO |
| S-05 | Feedback form: add star rating component instead of dropdown | MEDIUM | TODO |

### 8.2 Support Admin View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| S-06 | Ticket detail: add agent assignment dropdown | MEDIUM | TODO |
| S-07 | Stats tab: add visual charts (ticket volume, response time, satisfaction) | LOW | TODO |
| S-08 | Auto-responses: add test/preview feature | LOW | TODO |

---

## 9. Community Module

### 9.1 Community View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| C-01 | Post composer: add rich text or markdown support | LOW | TODO |
| C-02 | Feed posts: add image attachment support | MEDIUM | TODO |
| C-03 | Add like animation (heart pulse) on toggle | LOW | TODO |
| C-04 | Event cards: add cover image placeholder | LOW | TODO |
| C-05 | Messages: add online status indicator (green dot) | MEDIUM | TODO |
| C-06 | DM bubble colors now themed — verify visual | LOW | DONE |
| C-07 | Event type dropdown now shows title-cased names — verify | LOW | DONE |

### 9.2 Notifications View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| C-08 | Replace notification bell emoji with SVG icon (BELL fallback graphic) | HIGH | DONE |
| C-09 | Replace trash emoji with SVG icon (CHECK for markAll, TRASH for clear) | HIGH | DONE |
| C-10 | Add notification grouping by date (Today / Yesterday / Earlier) | MEDIUM | TODO |
| C-11 | Add slide-to-dismiss gesture or swipe-delete on notification items | LOW | TODO |

### 9.3 Reports View (Admin)
| ID | Task | Priority | Status |
|----|------|----------|--------|
| C-12 | Add report detail expansion panel | LOW | TODO |
| C-13 | Add bulk moderation actions | LOW | TODO |

---

## 10. Settings & Profile

### 10.1 Settings View
| ID | Task | Priority | Status |
|----|------|----------|--------|
| SP-01 | Add preview of theme change before applying | LOW | TODO |
| SP-02 | Add notification preferences section (email on/off, push on/off) | MEDIUM | TODO |
| SP-03 | Add account danger zone (delete account, export data) | LOW | TODO |

### 10.2 Profile Wizard
| ID | Task | Priority | Status |
|----|------|----------|--------|
| SP-04 | Bullet character fix — already done (`&#x2022;`) | LOW | DONE |
| SP-05 | Add profile photo upload with crop/resize | MEDIUM | TODO |
| SP-06 | Skills section: add autocomplete suggestions from a curated list | MEDIUM | TODO |
| SP-07 | Experience section: add rich date picker for start/end dates | LOW | TODO |

---

## 11. Cross-Cutting UX

### 11.1 Navigation
| ID | Task | Priority | Status |
|----|------|----------|--------|
| X-01 | Add breadcrumb trail for nested views (e.g., Feed > Job Details) | MEDIUM | TODO |
| X-02 | Add keyboard shortcut navigation (Ctrl+1..9 for sidebar items) | LOW | TODO |
| X-03 | Profile sidebar truncation — already fixed with ellipsis | LOW | DONE |

### 11.2 Accessibility
| ID | Task | Priority | Status |
|----|------|----------|--------|
| X-04 | Ensure all interactive elements have focus indicators (ring style) | LOW | TODO |
| X-05 | Add aria-label equivalents (accessibleText) on icon-only buttons | MEDIUM | TODO |

### 11.3 Error Handling
| ID | Task | Priority | Status |
|----|------|----------|--------|
| X-06 | Standardize error page icons — SVG (SEARCH, ALERT_TRIANGLE, WIFI, LOCK, 64px) | HIGH | DONE |
| X-07 | Add retry mechanism on network errors (auto-retry + manual button) | MEDIUM | TODO |

### 11.4 TRAINER Role
| ID | Task | Priority | Status |
|----|------|----------|--------|
| X-08 | Design dedicated TRAINER sidebar navigation (courses, mentorships, students, analytics) | LOW | TODO |
| X-09 | Add course creation/management view for trainers | LOW | TODO |

---

## Execution Priority

### Phase 8A — Immediate (This Session)
- **A-01**: Password visibility toggle in `TLPasswordField`
- **A-02**: Forgot password link position fix
- **G-01/G-02/G-03**: SVG icon system + replace ALL emojis
- **D-04**: Dashboard greeting emoji → SVG

### Phase 8B — High Impact UX
- **A-08**: Password strength indicator
- **A-12**: OTP digit boxes
- **G-06**: Exit animation on view transitions
- **R-02**: Card hover effects
- **S-03**: Chatbot typing indicator

### Phase 8C — Visual Polish
- **G-07/G-08**: Staggered cards + hover micro-interactions
- **D-01/D-02**: Dashboard icons and trend arrows
- **R-04/R-05**: Tag filter + bookmark animation

### Phase 8D — Feature Enhancements
- **A-09/A-10**: Registration UX improvements
- **R-17/R-18**: Post job wizard polish
- **FI-04**: Salary chart visualization
- **S-05**: Star rating component

---

*Last updated: 2025-07-16*
*Total items: 97 | Done: 32 | Remaining: 65*
