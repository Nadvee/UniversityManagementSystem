# Object-oriented project guide

The original folder structure is retained. Only the backend needed by the 13 role pages and authentication remains.

## Layers

Browser page classes call `ApiClient`. Controllers delegate to services. Services coordinate transactions, domain rules and repository methods. Repositories contain SQL. Shared configuration, security and database helpers remain in `shared/`.

| Part | Responsibility |
|---|---|
| `account/` | Accounts, password setup, authentication and dashboard assembly |
| `teacher/` | Select/confirm/remove course choices and assigned sections |
| `admin/` | Account directories, teacher requests, offerings and deletion |
| `advising/` | Windows, holds, academic validation and student workflows |
| `catalog/` | Course/section projections and cached advising/routine lists |
| `seat/` | Database/Redis allocation, release and transaction compensation |
| `drop/` | Semester-drop rules and enrollment changes |
| `seed/` | Administrator and reference course/room/rule initialization |
| `shared/` | Configuration, security, exceptions, values and audit events |

## OOP examples

- `User` has Student, Teacher and Admin subclasses.
- Student/Teacher account creators share the account-creation algorithm.
- `AdvisingValidator` composes prerequisite, clash, duplicate and credit rules.
- Database and Redis seat allocators implement the same interface. The Redis allocator delegates to the database guard and compensates failed reservations.
- Domain objects validate enrollment, hold, window, course-choice and time-slot transitions.
- Audit events are handled by a shared listener for the retained dashboard activity log.
- Frontend page classes retain their BasePage/StudentPage/TeacherPage/AdminPage hierarchy and reuse tables, forms, dialogs and the application shell.

## Data compatibility

The schema includes only 22 tables supporting the 13 role pages and authentication. Advising reads student CGPA and `prerequisite_records` for academic eligibility. Existing datasets are migrated with a local backup before obsolete tables are removed.

`docs/api-contract.json` and `scripts/check-architecture.mjs` verify the current routes, page paths, schema fingerprint and infrastructure boundaries. Behavioral tests cover the remaining account, selection, advising, routine, drop, cache and deletion flows.
