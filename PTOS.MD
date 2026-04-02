# PTOS

## What This Application Is

PTOS is a server-rendered personal training platform built for two user roles:

- `PT` users, who manage clients, create workouts, review check-ins, and monitor business/engagement signals
- `CLIENT` users, who complete their profile, view assigned workouts, submit check-ins, and track progress

At its current stage, PTOS is an MVP focused on the core PT-client workflow:

1. A PT has a roster of clients
2. The PT creates workouts
3. The PT assigns workouts to clients
4. Clients start and complete workouts
5. Clients submit periodic check-ins
6. The PT reviews those check-ins and sends feedback
7. The PT monitors client engagement and basic business metrics from the dashboard and business views

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
  - Aggregates repository data for dashboards, business views, suggested actions, and milestones
- `repository`
  - Spring Data JPA repositories
  - Encapsulate persistence access and query methods
- `domain`
  - JPA entities and enums
  - Represent users, client records, profiles, workouts, workout assignments, check-ins, and feedback
- `dto`
  - Form objects and view models
  - Used to keep controller/service interactions explicit and stable
- `security`
  - UserDetails integration
  - Role-based routing and current-user helpers
- `templates`
  - Thymeleaf views for PT and client pages
- `config`
  - Security setup
  - seed data

## Security Model

Security is role-based.

- Public routes:
  - `/`
  - `/login`
  - `/signup`
  - static assets
  - `/h2-console`
- PT-only routes:
  - `/pt/**`
- Client-only routes:
  - `/client/**`

After login, users are redirected by role:

- PT -> `/pt/dashboard`
- Client -> `/client/home`

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

### 2. ClientRecord

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

### 3. ClientProfile

Represents the client’s personal and goal data.

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

### 4. Workout

A PT-created reusable workout definition.

Key fields:

- PT owner
- name
- description
- category
- template flag

A workout contains ordered `WorkoutExercise` rows.

### 5. WorkoutAssignment

Represents a workout assigned to a specific client.

Key fields:

- workout
- client record
- assigned date
- status (`ASSIGNED`, `IN_PROGRESS`, `COMPLETED`)
- startedAt
- completedAt
- completionNotes

### 6. CheckIn

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

### 7. CheckInFeedback

PT feedback attached to a check-in.

Key fields:

- check-in
- PT user
- feedback text
- sentAt

## Derived Application Concepts

Some visible features are computed from existing data rather than stored directly.

### Suggested Actions

PT-facing reminders generated from rule-based logic, for example:

- pending check-ins
- at-risk clients
- clients without assigned workouts
- incomplete client profiles
- stale check-ins
- clients with no completed workouts

### Milestones

Derived from workout completion, check-in activity, profile completeness, and movement toward target weight.

Examples:

- First Workout Completed
- 3 Workouts Completed
- First Check-in Submitted
- Profile Complete
- Weight Milestone

### Business and Dashboard Metrics

Derived from client records, workout assignments, and check-ins.

Examples:

- active client count
- at-risk count
- pending check-ins
- recent pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- check-in compliance rate
- workout completion rate

## Seeded Demo Data

The app seeds a demo PT and a roster of demo clients on startup when the database is empty.

Seed characteristics:

- 1 PT account
- multiple client accounts
- client records linking those clients to the PT
- profiles with varied completeness
- workouts
- workout assignments in mixed states
- check-ins in mixed states
- some reviewed feedback

Current seeded PT login:

- email: `pt@ptos.local`
- password: `password123`

Seeded client accounts also use `password123`.

This seed behavior lives in [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java).

## User Roles and Capabilities

### PT Capabilities

A PT can currently:

- log in
- view the PT dashboard
- view business metrics
- browse all assigned clients
- search clients by name or email
- open an individual client detail page
- update client status
- update monthly package price
- update PT notes for a client
- view client profile completion and profile details
- view assigned workouts for a client
- view recent client check-ins
- view derived client milestones
- create a workout
- view workouts they have created
- open a workout detail page
- assign a workout to an active client
- browse pending/reviewed/all client check-ins
- open an individual check-in review page
- compare current and previous check-ins
- submit written feedback on a pending client check-in
- view suggested actions generated from engagement/business rules
- see recent milestone activity across clients

### Client Capabilities

A client can currently:

- sign up for a client account
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
- submit a new check-in
- view check-in details
- read PT feedback on reviewed check-ins
- view a progress card showing achieved milestones

## Important Current Limitation

Client self-signup creates a `CLIENT` user account only.

There is currently no UI workflow for:

- creating PT accounts from the app
- linking a newly signed-up client to a PT
- inviting a client into a PT’s roster

In practice, the full PT/client workflow currently depends on seeded demo data or manually created records in the database.

## Main PT Flow

This is the intended PT journey in the app today.

### 1. Log in

The PT logs in and lands on `/pt/dashboard`.

### 2. Review dashboard

The dashboard gives a quick operational snapshot:

- total clients
- active clients
- at-risk clients
- pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- recent pending check-ins
- recently updated clients
- suggested actions
- recent client milestone feed

### 3. Review client list

From `/pt/clients`, the PT can:

- see the full roster
- search by name or email
- open a specific client record

### 4. Work inside a client detail page

On `/pt/clients/{id}`, the PT can:

- review client overview and profile information
- see profile completion
- update status, package price, and PT notes
- review assigned workouts and completion state
- review recent check-ins
- inspect milestone progress

### 5. Create or assign workouts

The PT can:

- create a workout at `/pt/workouts/new`
- view workout details at `/pt/workouts/{id}`
- assign that workout to an active client from the workout detail page

### 6. Review check-ins

From `/pt/checkins`, the PT can:

- filter by pending, reviewed, or all
- open a single check-in
- compare current vs previous submission
- write and submit feedback

Submitting feedback marks the check-in as reviewed.

### 7. Monitor business health

From `/pt/business`, the PT can monitor:

- revenue
- average revenue per active client
- clients needing attention
- profile completion rate
- check-in compliance rate
- workout completion rate
- client-by-client health labels

## Main Client Flow

This is the intended client journey in the app today.

### 1. Sign up or log in

Clients can sign up at `/signup` and log in at `/login`.

After login, they land on `/client/home`.

### 2. Complete profile

From `/client/profile`, the client can enter:

- body metrics
- goal data
- injuries/limitations
- dietary preferences
- training experience
- general notes

Profile completeness is surfaced throughout the app.

### 3. View assigned workouts

From `/client/workouts`, the client sees workouts grouped into:

- Upcoming
- In Progress
- Completed

### 4. Perform workout

On `/client/workouts/{assignmentId}`:

- an `ASSIGNED` workout can be started
- an `IN_PROGRESS` workout can be completed with optional notes
- a `COMPLETED` workout shows started/completed times, duration, and notes

### 5. Submit check-ins

From `/client/checkins/new`, the client submits:

- current weight
- mood
- energy
- sleep quality
- notes

Submitting a check-in also updates `ClientProfile.currentWeightKg`.

### 6. Review check-in history

On `/client/checkins`, the client can browse previous submissions.

On `/client/checkins/{id}`, the client can:

- review the submitted data
- see whether it is pending or reviewed
- read PT feedback once available

### 7. Track progress

On `/client/home`, the client can see:

- today’s active workouts
- whether a recent weekly check-in exists
- milestone count
- recent achieved milestones

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
| `GET` | `/h2-console` | H2 console entry point |

### Client Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/client/home` | Client home dashboard |
| `GET` | `/client/profile` | View/edit client profile |
| `POST` | `/client/profile` | Save profile |
| `GET` | `/client/workouts` | List assigned workouts grouped by status |
| `GET` | `/client/workouts/{assignmentId}` | Workout assignment detail |
| `POST` | `/client/workouts/{assignmentId}/start` | Mark assignment as in progress |
| `POST` | `/client/workouts/{assignmentId}/complete` | Mark assignment as completed |
| `GET` | `/client/checkins` | List client’s check-ins |
| `GET` | `/client/checkins/new` | New check-in form |
| `POST` | `/client/checkins` | Submit new check-in |
| `GET` | `/client/checkins/{id}` | View one check-in and any PT feedback |

### PT Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/pt/dashboard` | PT dashboard |
| `GET` | `/pt/business` | Business analytics view |
| `GET` | `/pt/clients` | Client roster, optional search via `q` |
| `GET` | `/pt/clients/{id}` | Client detail |
| `POST` | `/pt/clients/{id}` | Update client status, package price, and PT notes |
| `GET` | `/pt/workouts` | PT workout library |
| `GET` | `/pt/workouts/new` | Create workout form |
| `POST` | `/pt/workouts` | Create workout |
| `GET` | `/pt/workouts/{id}` | Workout detail |
| `POST` | `/pt/workouts/{id}/assign` | Assign workout to a client |
| `GET` | `/pt/checkins` | List check-ins, optional `filter=pending|reviewed|all` |
| `GET` | `/pt/checkins/{id}` | Review one client check-in |
| `POST` | `/pt/checkins/{id}/feedback` | Submit PT feedback for a pending check-in |

## Controllers and Responsibilities

### AuthController

- client signup only

### HomeController

- public landing page
- login page
- client home page

### ClientProfileController

- create/update client profile

### ClientWorkoutController

- client workout list/detail/start/complete

### ClientCheckInController

- client check-in history, submission, and detail

### PtDashboardController

- PT dashboard
- business view

### PtClientsController

- PT client roster
- PT client detail
- PT-side client updates

### PtWorkoutController

- PT workout list
- workout creation
- workout detail
- workout assignment

### PtCheckInController

- PT check-in queue
- PT review page
- PT feedback submission

## Service Layer Summary

### UserService

- checks duplicate emails
- creates client accounts with encoded passwords

### ClientProfileService

- loads profiles
- creates or updates profile data

### ClientRecordService

- loads PT rosters and detail views
- updates PT-managed client settings
- computes profile completion for list/detail views

### WorkoutService

- creates workouts and exercise rows
- assigns workouts to clients
- loads client assignments
- starts and completes assignments

### CheckInService

- creates check-ins
- updates profile weight on check-in submission
- loads check-ins for PTs and clients
- submits PT feedback and marks a check-in reviewed

### DashboardService

- assembles PT dashboard counters and lists

### BusinessService

- assembles PT business metrics and client-level health indicators

### SuggestedActionsService

- produces PT action recommendations from fixed rules

### MilestoneService

- derives client milestones and recent milestone feed items

## Current PT Dashboard Content

The PT dashboard currently includes:

- total clients
- active clients
- at-risk clients
- pending check-ins
- workouts completed this week
- total workouts assigned
- estimated monthly revenue
- recent pending check-ins
- recent milestones
- recently updated clients
- quick links
- suggested actions

## Current Business View Content

The business page currently includes:

- estimated monthly revenue
- average revenue per active client
- active client count
- clients needing attention
- profile completion rate
- check-in compliance rate
- workout completion rate
- client breakdown table
- shortened suggested actions list

## Current Client Home Content

The client home page currently includes:

- welcome header
- profile completion card
- today’s workouts card
- check-in status card
- my progress / milestones card

## Navigation Structure

### PT Navigation

- Dashboard
- Clients
- Workouts
- Check-ins
- Business

The Check-ins nav item also shows a pending-count badge.

### Client Navigation

- Home
- My Workouts
- My Check-ins
- My Profile

## What The Application Does Not Yet Do

As of now, PTOS does not yet provide:

- PT account signup or PT management UI
- admin role or admin console
- client invitation/onboarding workflow from the PT UI
- automatic linking of new client signups to a PT
- PT creation, editing, or deletion of client records from the UI
- workout editing
- workout deletion
- unassigning a workout
- check-in editing after submission
- check-in reminders/notifications
- messaging/chat
- scheduling/calendar
- payments or invoicing
- file uploads
- multi-PT business management
- API endpoints for mobile or SPA clients

## Design Notes

The app is designed around a few practical ideas:

- keep the PT workflow simple and operational
- make the client workflow low-friction
- store only core records, and derive as much insight as possible from them
- use server-rendered HTML so the product can move quickly without a separate frontend application

This is why several features are derived rather than persisted:

- milestones
- suggested actions
- check-in compliance
- workout completion rate
- health labels

That keeps the current model relatively small while still giving the PT useful decision support.

## Good Starting Points For Future Contributors

If you are updating the app, these are the main files to begin with:

- App startup: [PtosApplication.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/PtosApplication.java)
- Security: [SecurityConfig.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/SecurityConfig.java)
- Shared layout/nav: [default.html](/Users/dev/Desktop/ptos/src/main/resources/templates/layout/default.html)
- PT dashboard flow: [PtDashboardController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtDashboardController.java)
- Client workflow: [HomeController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/HomeController.java), [ClientWorkoutController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientWorkoutController.java), [ClientCheckInController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/ClientCheckInController.java)
- PT workflow: [PtClientsController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtClientsController.java), [PtWorkoutController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtWorkoutController.java), [PtCheckInController.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/controller/PtCheckInController.java)
- Derived analytics and engagement logic: [DashboardService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/DashboardService.java), [BusinessService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/BusinessService.java), [SuggestedActionsService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/SuggestedActionsService.java), [MilestoneService.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/service/MilestoneService.java)
- Seed/demo data: [DataSeeder.java](/Users/dev/Desktop/ptos/src/main/java/com/ptos/config/DataSeeder.java)

## Short Summary

PTOS currently supports the core loop of online personal training:

- PT monitors clients
- PT creates and assigns workouts
- client completes workouts
- client submits check-ins
- PT reviews progress and sends feedback
- the app surfaces business and engagement signals on top of those core records

That is the product as it exists today.
