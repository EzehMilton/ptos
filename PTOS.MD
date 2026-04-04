# PTOS

## What This Application Is

PTOS (branded as **SETLY** in the UI) is a server-rendered personal training management platform built exclusively for PT users. Clients exist as managed data — they do not have web access to this application.

- `PT` users sign up, build a client roster, create workouts, assign meal plans, review check-ins, communicate via messaging, and monitor business/engagement signals
- `CLIENT` user accounts exist in the database for identity, but clients do not log in or interact with the application through a browser. Their only web touchpoint is accepting an invitation link to register.

At its current stage, PTOS covers the full PT management workflow:

1. A PT signs up and completes their business profile (onboarding)
2. The PT invites clients or creates client accounts directly
3. Clients accept invitations to register (their only web interaction)
4. The PT manages client profiles, status, and notes
5. The PT creates workouts and assigns them to clients
6. The PT creates and manages meal plans for clients
7. The PT reviews client check-ins and sends feedback
8. The PT communicates with clients via in-app messaging
9. The PT monitors client health scores, churn risk, and business metrics

This is a traditional Spring Boot MVC application. It does not expose a REST API. The app renders HTML on the server using Thymeleaf.

## Tech Stack

- Java / Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA / Hibernate
- Thymeleaf
- H2 in-memory database
- Lombok

## Runtime and Environment

- Default port: `8082`
- Database: in-memory H2
- H2 console: `/h2-console`
- Thymeleaf cache: disabled for development
- JPA schema mode: `update`
- Seed data: enabled by default via `ptos.seed-data: true`
- File upload: max 10MB per file, 30MB per request
- File storage: local filesystem at `./uploads` (configurable via `ptos.storage.local-dir`)

Current config is in [application.yml](/Users/dev/Desktop/ptos/src/main/resources/application.yml).

## High-Level Architecture

The codebase follows a standard Spring MVC layered structure:

- `controller`
  - Handles HTTP routes
  - Reads the current authenticated user
  - Validates forms
  - Delegates business logic to services
  - Returns Thymeleaf views
  - All controllers are PT-facing except InviteController (public)
- `service`
  - Contains business logic
  - Owns read/write workflows
  - Aggregates repository data for dashboards, business views, suggested actions, milestones, health scores, and insights
- `repository`
  - Spring Data JPA repositories
  - Encapsulate persistence access and query methods
- `domain`
  - JPA entities and enums
  - Represent users, PT profiles, client records, client record notes, client profiles, workouts, workout assignments, check-ins, check-in photos, feedback, conversations, messages, meal plans, compliance logs, and invitations
- `dto`
  - Form objects and view models
  - Used to keep controller/service interactions explicit and stable
- `security`
  - UserDetails integration
  - Role-based routing and current-user helpers
- `integration`
  - Gateway interfaces defining contracts for external concerns (AI insights, file storage, message delivery)
  - `integration.dummy` — rule-based stub implementations (e.g. insight generation without an AI provider)
  - `integration.local` — local implementations (e.g. filesystem storage, no-op message delivery)
- `templates`
  - Thymeleaf views — all under `pt/` (PT-facing) or `auth/` (public)
  - No client-facing templates exist
- `config`
  - Security setup
  - Seed data
  - File upload resource handler (WebConfig)

## Security Model

This is a PT-only application. Client accounts cannot log in.

- Public routes:
  - `/`
  - `/login`
  - `/pt/signup`
  - `/invite/**`
  - `/uploads/**`
  - static assets
  - `/h2-console`
- PT-only routes:
  - `/pt/onboarding`, `/pt/onboarding/**`
  - `/pt/**`
- All other requests: `permitAll()` (no client-gated routes exist)

After login:

- PT with completed onboarding -> `/pt/dashboard`
- PT without completed onboarding -> `/pt/onboarding`
- CLIENT attempting login -> session invalidated, redirected to `/login?clientAccess=true` with error message "Client accounts do not have web access in this application"

This behavior is defined in [SecurityConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/SecurityConfig.java).

## Core Domain Model

### 1. User

Represents a login account.

Fields include:

- `email`
- `password`
- `fullName`
- `role` (`PT` or `CLIENT`)
- `enabled`

CLIENT users exist for identity/linking purposes but cannot access the web UI.

### 2. PTProfile

Represents the PT's business profile, created during onboarding.

Key fields:

- `user` (one-to-one)
- `businessName`
- `specialisation`
- `location`
- `bio`
- `logoUrl`
- `onboardingComplete`

### 3. ClientRecord

Represents the PT-client relationship and PT-side management state.

Key fields:

- `ptUser`
- `clientUser`
- `status` (`ACTIVE`, `AT_RISK`, `INACTIVE`, `ARCHIVED`)
- `startDate`
- `monthlyPackagePrice`
- `ptNotes` (latest note text — also stored individually in ClientRecordNote)

Important points:

- This is the record that makes a client belong to a PT
- Deleting a client record cascades to all related data (conversations, messages, invitations, compliance logs, meal plans, assignments, notes, check-ins). If the client user is not linked to any other PT, the user account and profile are also deleted.

### 4. ClientRecordNote

Timestamped PT notes attached to a client record. Notes are stored individually rather than overwriting a single field, preserving the full history.

Key fields:

- `clientRecord` (many-to-one)
- `ptUser` (many-to-one)
- `noteText` (max 2000 chars)
- `createdAt`

### 5. ClientProfile

Represents the client's personal and goal data.

Key fields:

- age
- height
- current weight
- goal type
- target weight
- injuries/conditions
- dietary preferences
- training experience
- notes
- `onboardingComplete`

### 6. Workout

A PT-created reusable workout definition.

Key fields:

- PT owner
- name
- description
- category
- template flag

A workout contains ordered `WorkoutExercise` rows.

### 7. WorkoutAssignment

Represents a workout assigned to a specific client.

Key fields:

- workout
- client record
- assigned date
- status (`ASSIGNED`, `IN_PROGRESS`, `COMPLETED`)
- startedAt
- completedAt
- completionNotes

### 8. CheckIn

A client-submitted progress update.

Key fields:

- client record
- submittedAt
- currentWeightKg
- moodScore
- energyScore
- sleepScore
- notes
- status (`PENDING_REVIEW`, `REVIEWED`)
- `photos` (one-to-many list of `CheckInPhoto`)

### 9. CheckInPhoto

A progress photo attached to a check-in.

Key fields:

- check-in (many-to-one)
- `photoType` (`FRONT`, `SIDE`, `BACK`)
- `storageKey`
- `originalFilename`
- `fileSize`
- `uploadedAt`

### 10. CheckInFeedback

PT feedback attached to a check-in.

Key fields:

- check-in
- PT user
- feedback text
- sentAt

### 11. ClientInvitation

An invitation from a PT for a client to join the platform.

Key fields:

- `ptUser` (many-to-one)
- `email`
- `fullName`
- `token` (unique, used in invite link)
- `status` (`PENDING`, `ACCEPTED`, `EXPIRED`)
- `expiresAt` (7 days from creation)
- `acceptedAt`
- `createdAt`

### 12. Conversation

A messaging thread between a PT and a client.

Key fields:

- `ptUser` (many-to-one)
- `clientUser` (many-to-one)
- unique constraint on (ptUser, clientUser)
- `lastMessageAt`
- `unreadCountPt`
- `unreadCountClient`
- `createdAt`

### 13. Message

An individual message within a conversation.

Key fields:

- `conversation` (many-to-one)
- `sender` (many-to-one to User)
- `senderRole` (enum)
- `content` (max 2000 chars)
- `readAt`
- `createdAt`

### 14. MealPlan

A nutrition plan created by a PT for a specific client.

Key fields:

- `ptUser` (many-to-one)
- `clientRecord` (many-to-one)
- `title`
- `overview`
- `dailyGuidance`
- `active` (boolean — only one active per client at a time)
- `createdAt`
- `updatedAt`

### 15. MealComplianceLog

Daily meal plan adherence entry.

Key fields:

- `clientRecord` (many-to-one)
- `mealPlan` (many-to-one)
- unique constraint on (clientRecordId, logDate)
- `logDate`
- `compliance` (`FOLLOWED`, `PARTIALLY_FOLLOWED`, `NOT_FOLLOWED`)
- `notes`
- `loggedAt`

## Enums

- `Role`: `PT`, `CLIENT`
- `ClientStatus`: `ACTIVE`, `AT_RISK`, `INACTIVE`, `ARCHIVED`
- `AssignmentStatus`: `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`
- `CheckInStatus`: `PENDING_REVIEW`, `REVIEWED`
- `GoalType`: `WEIGHT_LOSS`, `MUSCLE_GAIN`, `STRENGTH`, `ENDURANCE`, `GENERAL_FITNESS`
- `InvitationStatus`: `PENDING`, `ACCEPTED`, `EXPIRED`
- `ComplianceLevel`: `FOLLOWED`, `PARTIALLY_FOLLOWED`, `NOT_FOLLOWED`
- `PhotoType`: `FRONT`, `SIDE`, `BACK`
- `RiskLevel`: `HEALTHY`, `WATCH`, `AT_RISK`, `CHURNING`

## Integration Layer (Gateway Pattern)

The application uses a gateway pattern to decouple core logic from external integrations. Each gateway has an interface and one or more implementations.

### ClientInsightGateway

Contract for generating client insights (check-in summaries, at-risk explanations, weekly summaries).

- Current implementation: `RuleBasedClientInsightGenerator` (deterministic, rule-based — replaceable with an AI provider like Claude)

### FileStorageGateway

Contract for file storage (store, load, delete, get URL).

- Current implementation: `LocalFileStorageService` (stores files on local filesystem with UUID-based naming and directory traversal protection)

### MessageDeliveryGateway

Contract for message delivery notifications (on message sent, on conversation read).

- Current implementation: `NoOpMessageDeliveryGateway` (no-op stub — replaceable with WebSocket, email, or push notifications)

## Derived Application Concepts

Some visible features are computed from existing data rather than stored directly.

### Health Score

A per-client composite score (0–100) computed by `HealthScoreService` from five components:

| Component | Max Points | Scoring Logic |
| --- | --- | --- |
| Profile completion | 10 | Scaled from 6 tracked fields (age, height, weight, goal, target weight, experience) |
| Check-in activity | 30 | Based on days since last check-in (≤7d = 30, ≤14d = 20, ≤21d = 10, else 0) |
| Workout activity | 30 | Based on 28-day workout completion rate (≥80% = 30, ≥50% = 20, >0% = 10, else 0) |
| Goal progress | 15 | Based on weight trend toward target (improving = 15, stable = 8, regressing = 0) |
| Communication | 15 | Based on days since last message (≤7d = 15, ≤14d = 10, ≤21d = 5, else 0) |

Risk level assignment:
- 70+ = `HEALTHY`
- 50–69 = `WATCH`
- 30–49 = `AT_RISK`
- <30 = `CHURNING`

The health score can also be computed as of a past date for trend comparison (improving vs slipping).

### Suggested Actions

PT-facing reminders generated from rule-based logic, for example:

- pending check-ins
- churning clients (urgent re-engagement)
- at-risk clients
- clients without assigned workouts
- incomplete client profiles
- stale check-ins (14+ days)
- clients with no completed workouts

Actions link to relevant pages (e.g. "Send Message" links to the messaging thread).

### Milestones

Derived from workout completion, check-in activity, profile completeness, and movement toward target weight.

Examples:

- First Workout Completed
- 3 Workouts Completed
- First Check-in Submitted
- Profile Complete
- Weight Milestone

### Insights

Generated by `InsightService` using the `ClientInsightGateway`:

- **Check-in insight**: comparison summary of current vs previous check-in
- **At-risk insight**: explanation of why a client is at risk
- **Weekly summary**: narrative of a client's weekly progress
- **Recent weekly summaries**: top N clients by latest activity with summaries (shown on PT dashboard)

### Business and Dashboard Metrics

Derived from client records, workout assignments, check-ins, health scores, and meal plans.

Dashboard metrics:
- total clients
- active clients
- churn risk count (at-risk + churning by health score)
- inactive/archived count
- health overview breakdown (healthy, watch, at risk, churning)
- pending check-ins
- recent pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- clients without package price
- recent milestones
- weekly summaries
- recently updated clients
- suggested actions

Business metrics:
- estimated monthly revenue
- average revenue per client
- retention rate (active + at-risk as % of total)
- estimated client lifetime value
- 6-month revenue trend (computed from client start dates and package prices)
- active client count
- churn risk count
- average health score
- health distribution counts (healthy, watch, at-risk, churning)
- clients improving / stable / slipping (with individual score deltas)
- top 5 revenue clients by package price
- profile completion rate
- check-in compliance rate
- workout completion rate
- meal plan coverage (% of active clients with a meal plan)
- client breakdown (with health score and risk level per client)
- re-engagement candidates (at-risk/churning clients sorted by score)
- incomplete profile clients

## Seeded Demo Data

The app seeds a demo PT and a roster of demo clients on startup when the database is empty.

Seed characteristics:

- 1 PT account with PT profile (business name: "Big Mike Fitness", specialisation, location, bio, onboarding marked complete)
- 17 client accounts with varied profiles (all onboarding marked complete)
- client records linking clients to the PT with mixed statuses (ACTIVE, AT_RISK, INACTIVE)
- workouts (Fat Loss Foundation, Strength Builder, Mobility Reset)
- workout assignments in mixed states (ASSIGNED, IN_PROGRESS, COMPLETED)
- check-ins with mixed states and progress photos (seeded for Jamie Chen's check-ins)
- reviewed feedback on some check-ins
- meal plans with compliance logs (active plans for Alex, Jamie, Nnuola, Mia; one legacy plan for Alex)
- client record notes (timestamped coaching observations for multiple clients)
- client invitations in mixed states (PENDING, ACCEPTED, EXPIRED)
- conversations with realistic multi-message threads between PT and clients (with varying read statuses)

Current seeded PT login:

- email: `pt@ptos.local`
- password: `password123`

Seeded client accounts also use `password123` but cannot log in to the web UI.

This seed behavior lives in [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java).

## PT Capabilities

A PT can currently:

- sign up for a PT account at `/pt/signup`
- complete business profile onboarding (or skip it)
- edit their business profile at `/pt/profile`
- log in and view the PT dashboard
- view business metrics and health analytics
- browse all assigned clients
- search clients by name or email
- invite clients via email (generates a shareable invite link with 7-day expiry)
- directly create client accounts with a temporary password
- view pending and past invitations
- open an individual client detail page
- update client status and monthly package price
- add timestamped coaching notes to client records (preserved as history)
- view client profile completion and profile details
- view client health score, risk level, and risk insight
- delete a client record (with name confirmation; cascading delete of all related data)
- create a workout
- view workouts they have created
- delete a workout (cascading delete of all assignments)
- open a workout detail page
- assign a workout to an active client with a specific date
- unassign a workout from a client
- create, edit, delete, and view meal plans for clients
- view client meal compliance logs and compliance rate
- browse pending/reviewed/all client check-ins
- open an individual check-in review page
- compare current and previous check-ins
- view progress photos on check-ins
- submit written feedback on a pending client check-in
- view suggested actions generated from health scores and engagement rules
- see recent milestone activity across clients
- see weekly client summaries
- send and receive messages with clients
- view unread message count in navigation
- view conversation inbox with all client threads
- start a new conversation from a client record

## Client Interaction Model

Clients do not log in or use this application. Their only web interaction is:

- Accepting an invitation link at `/invite/{token}` to create their account and link to a PT
- After accepting, they see a confirmation page. There is no further web access.

All other client data (profiles, check-ins, workouts, compliance logs, messages) is managed by the PT through the PT UI, or submitted through external channels (future mobile app, API, etc.).

## Main PT Flow

This is the intended PT journey in the app today.

### 1. Sign up and onboard

The PT signs up at `/pt/signup` and is auto-authenticated, then redirected to `/pt/onboarding` to set up their business profile (business name, specialisation, location, bio, logo URL). They can also skip onboarding.

### 2. Log in

The PT logs in and lands on `/pt/dashboard` (or `/pt/onboarding` if not yet completed).

### 3. Review dashboard

The dashboard gives a quick operational snapshot:

- total clients, active clients, churn risk count
- pending check-ins
- workouts completed this week, total workouts assigned
- estimated monthly revenue
- health overview (healthy / watch / at risk / churning breakdown)
- recent pending check-ins
- recent milestones
- weekly client summaries
- recently updated clients
- quick links (manage clients, business overview, review check-ins)
- suggested actions

When no clients exist, the dashboard links to "Add a new Client".

### 4. Add clients

From `/pt/clients/new`, the PT can:

- invite a client by email (generates a shareable link valid for 7 days)
- directly create a client account with a temporary password

From `/pt/clients`, the PT can view pending and past invitations.

### 5. Manage client roster

From `/pt/clients`, the PT can:

- see the full roster
- search by name or email
- open a specific client record

### 6. Work inside a client detail page

On `/pt/clients/{id}`, the PT can:

- review client overview and profile information
- see profile completion
- view health score, risk level, and risk insight
- update status and package price
- add coaching notes (preserved as timestamped history)
- review assigned workouts and completion state
- unassign workouts
- review recent check-ins
- inspect milestone progress
- view active meal plan
- open conversation with client
- delete the client record (with name confirmation)

### 7. Create, assign, or delete workouts

The PT can:

- create a workout at `/pt/workouts/new`
- view workout details at `/pt/workouts/{id}`
- assign that workout to an active client with a specific date
- unassign a workout from a client
- delete a workout (cascading to all assignments)

### 8. Manage nutrition

The PT can:

- create a meal plan for a client at `/pt/clients/{id}/nutrition/new`
- edit the active meal plan at `/pt/clients/{id}/nutrition/edit`
- delete the active meal plan
- view client meal compliance logs and compliance rate

### 9. Review check-ins

From `/pt/checkins`, the PT can:

- filter by pending, reviewed, or all
- open a single check-in
- compare current vs previous submission
- view progress photos
- write and submit feedback

Submitting feedback marks the check-in as reviewed.

### 10. Communicate with clients

From `/pt/messages`, the PT can:

- view inbox with all client conversations (ordered by last message)
- see unread count per conversation
- open a conversation thread
- send messages
- start a new conversation from a client record

### 11. Monitor business health

From `/pt/business`, the PT can monitor:

- revenue and estimated client lifetime value
- average revenue per active client
- churn risk count
- average health score
- clients improving vs stable vs slipping
- profile completion rate
- check-in compliance rate
- workout completion rate
- meal plan coverage
- client-by-client breakdown with health score and risk level
- re-engagement candidates
- incomplete profile clients

### 12. Edit profile

From `/pt/profile`, the PT can update their business name, specialisation, location, bio, and logo URL.

## Endpoint Reference

All endpoints below are server-rendered MVC routes unless noted otherwise.

### Public / Auth

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Landing page |
| `GET` | `/login` | Login page |
| `POST` | `/login` | Spring Security login processing |
| `POST` | `/logout` | Logout |
| `GET` | `/pt/signup` | PT signup form |
| `POST` | `/pt/signup` | Creates a new PT user account (auto-authenticates) |
| `GET` | `/invite/{token}` | Invitation acceptance form |
| `POST` | `/invite/{token}` | Accept invitation, create client account |
| `GET` | `/invite/{token}/accepted` | Invitation accepted confirmation page |
| `GET` | `/h2-console` | H2 console entry point |

### PT Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/pt/onboarding` | PT onboarding form |
| `POST` | `/pt/onboarding` | Save business profile |
| `GET` | `/pt/onboarding/skip` | Skip onboarding |
| `GET` | `/pt/profile` | Edit PT profile |
| `POST` | `/pt/profile` | Update PT profile |
| `GET` | `/pt/dashboard` | PT dashboard |
| `GET` | `/pt/business` | Business analytics view |
| `GET` | `/pt/clients` | Client roster, optional search via `q` |
| `GET` | `/pt/clients/new` | Add client page (invite or direct create) |
| `POST` | `/pt/clients/invite` | Send client invitation |
| `POST` | `/pt/clients/create-direct` | Directly create client account |
| `GET` | `/pt/clients/{id}` | Client detail |
| `POST` | `/pt/clients/{id}` | Update client status, package price, add notes |
| `GET` | `/pt/clients/{id}/delete` | Client deletion confirmation page |
| `POST` | `/pt/clients/{id}/delete` | Delete client record (with name confirmation) |
| `GET` | `/pt/clients/{clientRecordId}/nutrition` | View client meal plan and compliance |
| `GET` | `/pt/clients/{clientRecordId}/nutrition/new` | New meal plan form |
| `POST` | `/pt/clients/{clientRecordId}/nutrition` | Create meal plan |
| `GET` | `/pt/clients/{clientRecordId}/nutrition/edit` | Edit active meal plan form |
| `POST` | `/pt/clients/{clientRecordId}/nutrition/update` | Update active meal plan |
| `POST` | `/pt/clients/{clientRecordId}/nutrition/delete` | Delete active meal plan |
| `GET` | `/pt/workouts` | PT workout library |
| `GET` | `/pt/workouts/new` | Create workout form |
| `POST` | `/pt/workouts` | Create workout |
| `GET` | `/pt/workouts/{id}` | Workout detail |
| `POST` | `/pt/workouts/{id}/assign` | Assign workout to a client |
| `POST` | `/pt/workouts/{id}/delete` | Delete workout |
| `POST` | `/pt/workouts/assignments/{assignmentId}/delete` | Unassign workout |
| `GET` | `/pt/checkins` | List check-ins, optional `filter=pending\|reviewed\|all` |
| `GET` | `/pt/checkins/{id}` | Review one client check-in |
| `POST` | `/pt/checkins/{id}/feedback` | Submit PT feedback for a pending check-in |
| `GET` | `/pt/messages` | Inbox with conversation list |
| `GET` | `/pt/messages/{conversationId}` | View conversation with client |
| `POST` | `/pt/messages/{conversationId}` | Send message to client |
| `GET` | `/pt/messages/new/{clientRecordId}` | Start or open conversation with client |

## Controllers and Responsibilities

### AuthController

- PT signup (with auto-authentication and redirect to onboarding)
- No client signup exists

### HomeController

- public landing page
- login page

### InviteController

- invitation acceptance (unauthenticated)
- token validation, expiry handling
- account creation and PT linking
- post-acceptance confirmation page

### PTOnboardingController

- PT business profile setup
- skip onboarding option

### PtProfileController

- PT profile editing (separate from onboarding)

### PtDashboardController

- PT dashboard
- business view

### PtClientsController

- PT client roster
- PT client detail (with health score, risk insight, notes history, meal plan, conversation link)
- PT-side client updates (status, package price, notes)
- client invitation and direct creation
- client deletion (with name confirmation)

### PtWorkoutController

- PT workout list
- workout creation
- workout detail
- workout assignment with date
- workout deletion
- workout unassignment

### PtCheckInController

- PT check-in queue
- PT review page
- PT feedback submission

### PtNutritionController

- view client meal plan and compliance
- create, edit, and delete meal plans

### PtMessageController

- inbox with conversation list
- view and send messages in conversations
- start new conversations from client records

### GlobalModelAttributes

- provides `currentPath` for navigation highlighting
- provides `ptPendingCheckInCount` for check-in badge (PT only)
- provides `unreadMessageCount` for message badge (PT only)

## Service Layer Summary

### UserService

- checks duplicate emails
- creates PT accounts with encoded passwords

### PTProfileService

- loads PT profiles
- creates or updates PT business profile
- marks onboarding complete

### ClientProfileService

- loads profiles
- creates or updates profile data

### ClientRecordService

- loads PT rosters and detail views
- updates PT-managed client settings
- adds timestamped coaching notes (ClientRecordNote)
- retrieves notes history for a client record
- deletes client records with cascading cleanup (conversations, messages, invitations, compliance logs, meal plans, assignments, notes, check-ins, profile, user account if orphaned)
- computes profile completion for list/detail views

### ClientInvitationService

- creates 7-day expiring invite tokens
- creates client accounts directly (with temp password)
- accepts invitations (creates user if needed, links to PT)
- lists pending and all invitations
- handles duplicate prevention and token validation

### WorkoutService

- creates workouts and exercise rows
- assigns workouts to clients
- deletes workouts (cascading to assignments)
- unassigns workouts from clients

### CheckInService

- creates check-ins (with photo uploads)
- updates profile weight on check-in submission
- loads check-ins for PTs
- submits PT feedback and marks a check-in reviewed

### NutritionService

- creates meal plans (deactivates previous active plan)
- updates active meal plans
- deletes meal plans (cascading to compliance logs)
- logs daily meal compliance
- retrieves compliance logs and compliance rate
- calculates meal plan coverage across PT's clients

### MessagingService

- creates or retrieves conversations (lazy initialization)
- lists conversations for PTs
- sends messages and updates conversation state
- marks messages as read and updates unread counts
- provides unread count for navigation badges
- formats relative timestamps

### HealthScoreService

- calculates per-client health scores (0–100) from 5 components
- assigns risk levels (HEALTHY, WATCH, AT_RISK, CHURNING)
- supports historical score calculation for trend comparison

### InsightService

- generates check-in comparison summaries
- generates at-risk explanations
- generates weekly progress narratives
- retrieves recent weekly summaries for dashboard

### DashboardService

- assembles PT dashboard counters, health overview, and lists

### BusinessService

- assembles PT business metrics, client health analytics, and re-engagement candidates
- computes 6-month revenue trend from client start dates and current package prices
- computes retention rate, health distribution counts, individual improving/slipping clients with score deltas, and top revenue clients

### SuggestedActionsService

- produces PT action recommendations from health scores and fixed rules
- prioritized and capped at 10 actions

### MilestoneService

- derives client milestones and recent milestone feed items

## Current PT Dashboard Content

The PT dashboard currently includes:

- total clients
- active clients
- churn risk count
- pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- health overview (healthy / watch / at risk / churning)
- recent pending check-ins
- recent milestones
- weekly client summaries
- recently updated clients
- quick links (manage clients, business overview, review check-ins)
- suggested actions

## Current Business View Content

The business page uses a dark-themed dashboard layout with a two-column card grid. It currently includes:

- KPI row: monthly revenue, revenue per client, retention rate, estimated client lifetime value
- Revenue trend (6 months): bar chart computed from client start dates and current package prices, with the current month highlighted
- Improving vs slipping: pill summary (N improving / N stable / N slipping) plus individual client rows showing name, current health score, and score delta with trend arrows
- Client health distribution: CSS donut chart showing healthy, watch, and at-risk/churning counts with center total
- Revenue by client: top 5 active clients sorted by package price descending
- Re-engagement opportunities: at-risk/churning clients with avatar initials, score, last activity, and "Send message" action
- Incomplete profiles: clients with fewer than 4 of 6 profile fields completed

## Navigation Structure

### PT Navigation (authenticated)

- Dashboard
- Clients
- Workouts
- Check-ins (with pending-count badge)
- Messages (with unread-count badge)
- Business
- Profile icon (links to `/pt/profile`)
- Logout button

### Unauthenticated Navigation

- Login
- PT Sign Up

There is no client navigation. Clients cannot access the web UI.

## What The Application Does Not Yet Do

As of now, PTOS does not yet provide:

- client-facing web UI (clients cannot log in or interact with the app)
- admin role or admin console
- automatic email sending for invitations (invite links are generated for manual sharing)
- real-time messaging (WebSocket/SSE)
- push notifications or email notifications
- workout editing (only creation and deletion)
- check-in editing after submission
- check-in reminders/notifications
- scheduling/calendar
- payments or invoicing
- multi-PT business management
- API endpoints for mobile or SPA clients
- AI-powered insights (currently uses rule-based stub; gateway interface ready for AI integration)

## Visual Theme

All authenticated PT pages (dashboard, clients, workouts, check-ins, messages, business) use a dark theme (`dashboard-page-body` CSS class) with dark card backgrounds, light text, and styled form elements. The landing page and PT signup page have their own distinct themes. A `<meta name="color-scheme" content="light">` tag is set on the shared layout to prevent browser-level dark mode from interfering with the app's own styling.

## Design Notes

The app is designed around a few practical ideas:

- this is a PT management tool — the PT is the only web user
- clients exist as data, not as active platform users
- keep the PT workflow simple and operational
- store only core records, and derive as much insight as possible from them
- use server-rendered HTML so the product can move quickly without a separate frontend application
- use a gateway pattern for external integrations so implementations can be swapped without changing core logic

This is why several features are derived rather than persisted:

- health scores and risk levels
- milestones
- suggested actions
- check-in compliance
- workout completion rate
- insights and weekly summaries

That keeps the current model relatively small while still giving the PT useful decision support.

The gateway pattern enables future integration with:
- AI providers (e.g. Claude) for smarter client insights
- Cloud storage (e.g. S3) for file hosting
- Real-time messaging (e.g. WebSocket) for notifications
- A future client-facing mobile app or API

## Good Starting Points For Future Contributors

If you are updating the app, these are the main files to begin with:

- App startup: [PtosApplication.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/PtosApplication.java)
- Security: [SecurityConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/SecurityConfig.java)
- Web config (file uploads): [WebConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/WebConfig.java)
- Shared layout/nav: [default.html](/Users/dev/Desktop/ptos/src/main/resources/templates/layout/default.html)
- PT dashboard flow: [PtDashboardController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtDashboardController.java)
- PT workflow: [PtClientsController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtClientsController.java), [PtWorkoutController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtWorkoutController.java), [PtCheckInController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtCheckInController.java)
- Messaging: [PtMessageController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtMessageController.java), [MessagingService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/MessagingService.java)
- Nutrition: [PtNutritionController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtNutritionController.java), [NutritionService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/NutritionService.java)
- Client acquisition: [InviteController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/InviteController.java), [ClientInvitationService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/ClientInvitationService.java)
- PT profile: [PtProfileController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtProfileController.java), [PTProfileService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/PTProfileService.java)
- Health scoring: [HealthScoreService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/HealthScoreService.java)
- Client record management: [ClientRecordService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/ClientRecordService.java)
- Derived analytics and engagement logic: [DashboardService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/DashboardService.java), [BusinessService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/BusinessService.java), [SuggestedActionsService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/SuggestedActionsService.java), [MilestoneService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/MilestoneService.java), [InsightService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/InsightService.java)
- Integration gateways: [ClientInsightGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/ClientInsightGateway.java), [FileStorageGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/FileStorageGateway.java), [MessageDeliveryGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/MessageDeliveryGateway.java)
- Seed/demo data: [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java)

## Short Summary

PTOS (branded as SETLY) is a PT-only management platform. Clients do not have web access.

The PT can:

- sign up, onboard, and manage their business profile
- invite or directly create client accounts
- manage a client roster with status tracking, coaching notes, and health scoring
- create, assign, unassign, and delete workouts
- create, edit, and delete meal plans with compliance tracking
- review client check-ins with progress photos and send feedback
- communicate with clients via in-app messaging
- monitor business health through dashboards with revenue, health scores, risk levels, and engagement signals

Client accounts exist for identity and data linking. Their only web interaction is accepting an invitation. All other client data is managed through the PT interface.

That is the product as it exists today.