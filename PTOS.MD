# PTOS

## What This Application Is

PTOS (branded as **SETLY** in the UI) is a server-rendered personal training platform built for two user roles:

- `PT` users, who manage clients, create workouts, review check-ins, manage nutrition plans, communicate via messaging, and monitor business/engagement signals
- `CLIENT` users, who complete their profile, view assigned workouts, submit check-ins, follow meal plans, message their PT, and track progress

At its current stage, PTOS covers the core PT-client workflow plus client acquisition, messaging, nutrition tracking, and health scoring:

1. A PT signs up and completes their business profile (onboarding)
2. The PT invites clients or creates client accounts directly
3. Clients accept invitations and complete their profile (onboarding)
4. The PT creates workouts and assigns them to clients
5. The PT creates meal plans for clients
6. Clients start and complete workouts
7. Clients log daily meal compliance
8. Clients submit periodic check-ins (with optional progress photos)
9. The PT reviews those check-ins and sends feedback
10. The PT and client communicate via in-app messaging
11. The PT monitors client health scores, churn risk, and business metrics from the dashboard and business views

This is a traditional Spring Boot MVC application. It does not currently expose a REST API. The app renders HTML on the server using Thymeleaf.

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
- `service`
  - Contains business logic
  - Owns read/write workflows
  - Aggregates repository data for dashboards, business views, suggested actions, milestones, health scores, and insights
- `repository`
  - Spring Data JPA repositories
  - Encapsulate persistence access and query methods
- `domain`
  - JPA entities and enums
  - Represent users, client records, profiles, PT profiles, workouts, workout assignments, check-ins, feedback, conversations, messages, meal plans, compliance logs, invitations, and check-in photos
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
  - Thymeleaf views for PT and client pages
- `config`
  - Security setup
  - Seed data
  - File upload resource handler (WebConfig)

## Security Model

Security is role-based.

- Public routes:
  - `/`
  - `/login`
  - `/signup`
  - `/pt/signup`
  - `/invite/**`
  - `/uploads/**`
  - static assets
  - `/h2-console`
- PT-only routes:
  - `/pt/onboarding`, `/pt/onboarding/**`
  - `/pt/**`
- Client-only routes:
  - `/client/**`

After login, users are redirected based on role and onboarding status:

- PT with completed profile -> `/pt/dashboard`
- PT without completed profile -> `/pt/onboarding`
- Client with completed profile -> `/client/home`
- Client without completed profile -> `/client/onboarding`

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
- `ptNotes`

Important point:

- This is the record that makes a client belong to a PT
- A client account can exist without a `ClientRecord`, but many client features assume one exists

### 4. ClientProfile

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

### 5. Workout

A PT-created reusable workout definition.

Key fields:

- PT owner
- name
- description
- category
- template flag

A workout contains ordered `WorkoutExercise` rows.

### 6. WorkoutAssignment

Represents a workout assigned to a specific client.

Key fields:

- workout
- client record
- assigned date
- status (`ASSIGNED`, `IN_PROGRESS`, `COMPLETED`)
- startedAt
- completedAt
- completionNotes

### 7. CheckIn

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

### 8. CheckInPhoto

A progress photo attached to a check-in.

Key fields:

- check-in (many-to-one)
- `photoType` (`FRONT`, `SIDE`, `BACK`)
- `storageKey`
- `originalFilename`
- `fileSize`
- `uploadedAt`

### 9. CheckInFeedback

PT feedback attached to a check-in.

Key fields:

- check-in
- PT user
- feedback text
- sentAt

### 10. ClientInvitation

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

### 11. Conversation

A messaging thread between a PT and a client.

Key fields:

- `ptUser` (many-to-one)
- `clientUser` (many-to-one)
- unique constraint on (ptUser, clientUser)
- `lastMessageAt`
- `unreadCountPt`
- `unreadCountClient`
- `createdAt`

### 12. Message

An individual message within a conversation.

Key fields:

- `conversation` (many-to-one)
- `sender` (many-to-one to User)
- `senderRole` (enum)
- `content` (max 2000 chars)
- `readAt`
- `createdAt`

### 13. MealPlan

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

### 14. MealComplianceLog

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
- estimated client lifetime value
- active client count
- churn risk count
- average health score
- clients improving / stable / slipping (trend comparison)
- profile completion rate
- check-in compliance rate
- workout completion rate
- meal plan coverage (% of active clients with a meal plan)
- client breakdown table (with health score and risk level per client)
- re-engagement candidates (at-risk/churning clients with 14–60 days of inactivity)
- incomplete profile clients

## Seeded Demo Data

The app seeds a demo PT and a roster of demo clients on startup when the database is empty.

Seed characteristics:

- 1 PT account with PT profile (business name, specialisation, location, bio, onboarding marked complete)
- multiple client accounts
- client records linking those clients to the PT
- profiles with varied completeness (onboarding marked complete on seeded clients)
- workouts
- workout assignments in mixed states
- check-ins in mixed states
- some reviewed feedback
- conversations with realistic multi-message threads between PT and clients (with varying read statuses)

Current seeded PT login:

- email: `pt@ptos.local`
- password: `password123`

Seeded client accounts also use `password123`.

This seed behavior lives in [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java).

## User Roles and Capabilities

### PT Capabilities

A PT can currently:

- sign up for a PT account at `/pt/signup`
- complete business profile onboarding (or skip it)
- log in
- view the PT dashboard
- view business metrics and health analytics
- browse all assigned clients
- search clients by name or email
- invite clients via email (generates a shareable invite link with 7-day expiry)
- directly create client accounts with a temporary password
- view pending and past invitations
- open an individual client detail page
- update client status
- update monthly package price
- update PT notes for a client
- view client profile completion and profile details
- view assigned workouts for a client
- view recent client check-ins
- view derived client milestones
- view client health score and risk level
- create a workout
- view workouts they have created
- open a workout detail page
- assign a workout to an active client
- create, edit, and view meal plans for clients
- view client meal compliance logs and compliance rate
- browse pending/reviewed/all client check-ins
- open an individual check-in review page
- compare current and previous check-ins
- submit written feedback on a pending client check-in
- view suggested actions generated from health scores and engagement rules
- see recent milestone activity across clients
- see weekly client summaries
- send and receive messages with clients
- view unread message count in navigation
- view conversation inbox with all client threads

### Client Capabilities

A client can currently:

- sign up for a client account
- accept an invitation from a PT (creates account if needed, links to PT)
- complete profile onboarding
- log in
- view their client home page
- complete or update their profile
- see profile completion progress
- view assigned workouts by status
- open a workout detail page
- start an assigned workout
- complete an in-progress workout
- submit completion notes
- browse their check-in history
- submit a new check-in (with optional progress photos)
- view check-in details
- read PT feedback on reviewed check-ins
- view a progress card showing achieved milestones
- view their active meal plan
- log daily meal compliance (followed, partially followed, not followed) with notes
- view recent compliance history
- send and receive messages with their PT
- view unread message count in navigation

## Main PT Flow

This is the intended PT journey in the app today.

### 1. Sign up and onboard

The PT signs up at `/pt/signup` and is auto-authenticated, then redirected to `/pt/onboarding` to set up their business profile (business name, specialisation, location, bio, logo URL). They can also skip onboarding.

### 2. Log in

The PT logs in and lands on `/pt/dashboard` (or `/pt/onboarding` if not yet completed).

### 3. Review dashboard

The dashboard gives a quick operational snapshot:

- total clients
- active clients
- churn risk count
- pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- health overview (healthy / watch / at risk / churning breakdown)
- recent pending check-ins
- recent milestones
- weekly client summaries
- recently updated clients
- quick links
- suggested actions

### 4. Add clients

From `/pt/clients`, the PT can:

- invite a client by email (generates a shareable link)
- directly create a client account with a temporary password
- view pending and past invitations

### 5. Review client list

From `/pt/clients`, the PT can:

- see the full roster
- search by name or email
- open a specific client record

### 6. Work inside a client detail page

On `/pt/clients/{id}`, the PT can:

- review client overview and profile information
- see profile completion
- update status, package price, and PT notes
- review assigned workouts and completion state
- review recent check-ins
- inspect milestone progress
- view/create/edit meal plans
- view meal compliance logs

### 7. Create or assign workouts

The PT can:

- create a workout at `/pt/workouts/new`
- view workout details at `/pt/workouts/{id}`
- assign that workout to an active client from the workout detail page

### 8. Manage nutrition

The PT can:

- create a meal plan for a client at `/pt/clients/{id}/nutrition/new`
- edit the active meal plan at `/pt/clients/{id}/nutrition/edit`
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

## Main Client Flow

This is the intended client journey in the app today.

### 1. Sign up or accept invitation

Clients can sign up at `/signup`, or accept a PT's invitation at `/invite/{token}`.

After accepting an invitation, the client account is created (if it doesn't exist) and linked to the PT.

### 2. Onboard

After first login, clients are redirected to `/client/onboarding` to complete their profile (age, height, weight, goals, training experience, injuries, dietary preferences).

### 3. Log in

Clients log in at `/login` and land on `/client/home` (or `/client/onboarding` if not yet completed).

### 4. Complete profile

From `/client/profile`, the client can update:

- body metrics
- goal data
- injuries/limitations
- dietary preferences
- training experience
- general notes

Profile completeness is surfaced throughout the app.

### 5. View assigned workouts

From `/client/workouts`, the client sees workouts grouped into:

- Upcoming
- In Progress
- Completed

### 6. Perform workout

On `/client/workouts/{assignmentId}`:

- an `ASSIGNED` workout can be started
- an `IN_PROGRESS` workout can be completed with optional notes
- a `COMPLETED` workout shows started/completed times, duration, and notes

### 7. Submit check-ins

From `/client/checkins/new`, the client submits:

- current weight
- mood
- energy
- sleep quality
- notes
- optional progress photos (front, side, back — JPEG/PNG/WEBP, max 10MB each)

Submitting a check-in also updates `ClientProfile.currentWeightKg`.

### 8. Review check-in history

On `/client/checkins`, the client can browse previous submissions.

On `/client/checkins/{id}`, the client can:

- review the submitted data
- see whether it is pending or reviewed
- read PT feedback once available

### 9. Follow meal plan

From `/client/nutrition`, the client can:

- view their active meal plan (title, overview, daily guidance)
- log daily meal compliance (followed / partially followed / not followed) with notes
- view recent compliance history (last 14 days)

### 10. Message PT

From `/client/messages`, the client can:

- view their conversation thread with their PT
- send and receive messages
- see unread message indicator in navigation

### 11. Track progress

On `/client/home`, the client can see:

- profile completion card
- today's active workouts
- check-in status card (due or submitted)
- meal plan card (active plan and today's compliance status)
- milestone count and recent achieved milestones

## Endpoint Reference

All endpoints below are server-rendered MVC routes unless noted otherwise.

### Public / Auth

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Landing page |
| `GET` | `/login` | Login page |
| `POST` | `/login` | Spring Security login processing |
| `POST` | `/logout` | Logout |
| `GET` | `/signup` | Client signup form |
| `POST` | `/signup` | Creates a new client user account |
| `GET` | `/pt/signup` | PT signup form |
| `POST` | `/pt/signup` | Creates a new PT user account (auto-authenticates) |
| `GET` | `/invite/{token}` | Invitation acceptance form |
| `POST` | `/invite/{token}` | Accept invitation, create account if needed |
| `GET` | `/h2-console` | H2 console entry point |

### Client Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/client/onboarding` | Client onboarding form |
| `POST` | `/client/onboarding` | Save onboarding profile, redirect to home |
| `GET` | `/client/home` | Client home dashboard |
| `GET` | `/client/profile` | View/edit client profile |
| `POST` | `/client/profile` | Save profile |
| `GET` | `/client/workouts` | List assigned workouts grouped by status |
| `GET` | `/client/workouts/{assignmentId}` | Workout assignment detail |
| `POST` | `/client/workouts/{assignmentId}/start` | Mark assignment as in progress |
| `POST` | `/client/workouts/{assignmentId}/complete` | Mark assignment as completed |
| `GET` | `/client/checkins` | List client's check-ins |
| `GET` | `/client/checkins/new` | New check-in form |
| `POST` | `/client/checkins` | Submit new check-in |
| `GET` | `/client/checkins/{id}` | View one check-in and any PT feedback |
| `GET` | `/client/nutrition` | View active meal plan and compliance logs |
| `POST` | `/client/nutrition/log` | Log daily meal compliance |
| `GET` | `/client/messages` | View conversation with PT |
| `POST` | `/client/messages` | Send message to PT |

### PT Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/pt/onboarding` | PT onboarding form |
| `POST` | `/pt/onboarding` | Save business profile |
| `GET` | `/pt/onboarding/skip` | Skip onboarding |
| `GET` | `/pt/dashboard` | PT dashboard |
| `GET` | `/pt/business` | Business analytics view |
| `GET` | `/pt/clients` | Client roster, optional search via `q` |
| `GET` | `/pt/clients/new` | Add client page (invite or direct create) |
| `POST` | `/pt/clients/invite` | Send client invitation |
| `POST` | `/pt/clients/create-direct` | Directly create client account |
| `GET` | `/pt/clients/{id}` | Client detail |
| `POST` | `/pt/clients/{id}` | Update client status, package price, and PT notes |
| `GET` | `/pt/clients/{clientRecordId}/nutrition` | View client meal plan and compliance |
| `GET` | `/pt/clients/{clientRecordId}/nutrition/new` | New meal plan form |
| `POST` | `/pt/clients/{clientRecordId}/nutrition` | Create meal plan |
| `GET` | `/pt/clients/{clientRecordId}/nutrition/edit` | Edit active meal plan form |
| `POST` | `/pt/clients/{clientRecordId}/nutrition/update` | Update active meal plan |
| `GET` | `/pt/workouts` | PT workout library |
| `GET` | `/pt/workouts/new` | Create workout form |
| `POST` | `/pt/workouts` | Create workout |
| `GET` | `/pt/workouts/{id}` | Workout detail |
| `POST` | `/pt/workouts/{id}/assign` | Assign workout to a client |
| `GET` | `/pt/checkins` | List check-ins, optional `filter=pending\|reviewed\|all` |
| `GET` | `/pt/checkins/{id}` | Review one client check-in |
| `POST` | `/pt/checkins/{id}/feedback` | Submit PT feedback for a pending check-in |
| `GET` | `/pt/messages` | Inbox with conversation list |
| `GET` | `/pt/messages/{conversationId}` | View conversation with client |
| `POST` | `/pt/messages/{conversationId}` | Send message to client |
| `GET` | `/pt/messages/new/{clientRecordId}` | Start or open conversation with client |

## Controllers and Responsibilities

### AuthController

- client signup
- PT signup (with auto-authentication and redirect to onboarding)

### HomeController

- public landing page
- login page
- client home page

### InviteController

- invitation acceptance (unauthenticated)
- token validation, expiry handling
- account creation and PT linking

### PTOnboardingController

- PT business profile setup
- skip onboarding option

### ClientOnboardingController

- client profile onboarding
- redirect to home after completion

### ClientProfileController

- create/update client profile

### ClientWorkoutController

- client workout list/detail/start/complete

### ClientCheckInController

- client check-in history, submission (with photo upload), and detail

### ClientNutritionController

- view active meal plan
- log daily meal compliance

### ClientMessageController

- view and send messages in conversation with PT

### PtDashboardController

- PT dashboard
- business view

### PtClientsController

- PT client roster
- PT client detail
- PT-side client updates
- client invitation and direct creation

### PtWorkoutController

- PT workout list
- workout creation
- workout detail
- workout assignment

### PtCheckInController

- PT check-in queue
- PT review page
- PT feedback submission

### PtNutritionController

- view client meal plan and compliance
- create and edit meal plans

### PtMessageController

- inbox with conversation list
- view and send messages in conversations
- start new conversations from client records

### GlobalModelAttributes

- provides `currentPath` for navigation highlighting
- provides `ptPendingCheckInCount` for check-in badge
- provides `unreadMessageCount` for message badge (both PT and client)

## Service Layer Summary

### UserService

- checks duplicate emails
- creates client accounts with encoded passwords
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
- loads client assignments
- starts and completes assignments

### CheckInService

- creates check-ins (with photo uploads)
- updates profile weight on check-in submission
- loads check-ins for PTs and clients
- submits PT feedback and marks a check-in reviewed

### NutritionService

- creates meal plans (deactivates previous active plan)
- updates active meal plans
- logs daily meal compliance
- retrieves compliance logs and compliance rate
- calculates meal plan coverage across PT's clients

### MessagingService

- creates or retrieves conversations (lazy initialization)
- lists conversations for PTs and clients
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

The business page currently includes:

- estimated monthly revenue
- average revenue per active client
- estimated client lifetime value
- active client count
- churn risk count
- average health score
- client overview table (with health score bar, risk level badge per client)
- re-engagement opportunities (at-risk/churning clients with 14–60 day inactivity)
- improving vs stable vs slipping breakdown

## Current Client Home Content

The client home page currently includes:

- welcome header
- profile completion card
- today's workouts card
- check-in status card (due or submitted)
- meal plan card (active plan and today's compliance)
- my progress / milestones card
- quick links (workouts, check-in, nutrition)

## Navigation Structure

### PT Navigation

- Dashboard
- Clients
- Workouts
- Check-ins (with pending-count badge)
- Messages (with unread-count badge)
- Business

### Client Navigation

- Home
- My Workouts
- My Check-ins
- Nutrition
- Messages (with unread-count badge)
- My Profile

## What The Application Does Not Yet Do

As of now, PTOS does not yet provide:

- admin role or admin console
- automatic email sending for invitations (invite links are generated for manual sharing)
- real-time messaging (WebSocket/SSE)
- push notifications or email notifications
- workout editing
- workout deletion
- unassigning a workout
- check-in editing after submission
- check-in reminders/notifications
- scheduling/calendar
- payments or invoicing
- multi-PT business management
- API endpoints for mobile or SPA clients
- AI-powered insights (currently uses rule-based stub; gateway interface ready for AI integration)

## Design Notes

The app is designed around a few practical ideas:

- keep the PT workflow simple and operational
- make the client workflow low-friction
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

## Good Starting Points For Future Contributors

If you are updating the app, these are the main files to begin with:

- App startup: [PtosApplication.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/PtosApplication.java)
- Security: [SecurityConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/SecurityConfig.java)
- Web config (file uploads): [WebConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/WebConfig.java)
- Shared layout/nav: [default.html](/Users/dev/Desktop/ptos/src/main/resources/templates/layout/default.html)
- PT dashboard flow: [PtDashboardController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtDashboardController.java)
- Client workflow: [HomeController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/HomeController.java), [ClientWorkoutController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientWorkoutController.java), [ClientCheckInController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientCheckInController.java)
- PT workflow: [PtClientsController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtClientsController.java), [PtWorkoutController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtWorkoutController.java), [PtCheckInController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtCheckInController.java)
- Messaging: [PtMessageController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtMessageController.java), [ClientMessageController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientMessageController.java), [MessagingService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/MessagingService.java)
- Nutrition: [PtNutritionController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtNutritionController.java), [ClientNutritionController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientNutritionController.java), [NutritionService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/NutritionService.java)
- Client acquisition: [InviteController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/InviteController.java), [ClientInvitationService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/ClientInvitationService.java)
- Health scoring: [HealthScoreService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/HealthScoreService.java)
- Derived analytics and engagement logic: [DashboardService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/DashboardService.java), [BusinessService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/BusinessService.java), [SuggestedActionsService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/SuggestedActionsService.java), [MilestoneService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/MilestoneService.java), [InsightService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/InsightService.java)
- Integration gateways: [ClientInsightGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/ClientInsightGateway.java), [FileStorageGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/FileStorageGateway.java), [MessageDeliveryGateway.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/integration/MessageDeliveryGateway.java)
- Seed/demo data: [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java)

## Short Summary

PTOS (branded as SETLY) currently supports the full loop of online personal training:

- PT signs up and sets up business profile
- PT invites clients or creates client accounts
- clients onboard and complete their profile
- PT creates and assigns workouts
- PT creates meal plans for clients
- client completes workouts and logs meal compliance
- client submits check-ins with optional progress photos
- PT reviews progress and sends feedback
- PT and client communicate via in-app messaging
- the app surfaces health scores, risk levels, business metrics, insights, and engagement signals on top of those core records

That is the product as it exists today.