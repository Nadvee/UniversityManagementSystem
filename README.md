# University of Mirpur — Course Selection & Advising

Java 17 / Spring Boot application. The existing feature folders and controller → service → repository structure are preserved.

The application has these 13 role pages:

| Teacher | Admin | Student |
|---|---|---|
| Dashboard | Dashboard | Dashboard |
| Course Selection | Teachers | Advising |
| My Courses | Students | Class Routine |
| | Teacher Requests | Semester Drop |
| | Course Offerings | |
| | Advising Control | |

Only these 13 role pages and authentication are included. Login and first-time password setup remain available.

## Run locally

Java 17+ and Docker Desktop are required for the configured MySQL setup. Database settings are read from `.env.mysql.local`; copy `.env.mysql.example` and fill in the passwords on a new computer. Keep the private settings file out of source submissions. Existing database records are kept in the Docker volume.

| Launcher | Action |
|---|---|
| **RUN-LOCAL.cmd** | Starts MySQL if needed, starts the website and opens `http://localhost:8081/login.html`. |
| **RUN-DB.cmd** | Starts MySQL if needed and opens phpMyAdmin at `http://localhost:8082`. |
| **STOP-LOCAL.cmd** | Stops this project's website. |
| **STOP-DB.cmd** | Stops phpMyAdmin. MySQL remains available to the website. |

The website runs in the background; its logs are saved in `data/logs/`. Closing a browser tab does not stop either server. An optional website port can be selected with `RUN-LOCAL.cmd -Port 8083`; port 8082 is reserved for phpMyAdmin.

Default administrator: ID `admin`, password `admin`. `ADMIN_PASSWORD` can override the configured password. Existing configured account passwords and database records are retained. Newly created teacher/student accounts use **First time? Set your password** before logging in.

The website and phpMyAdmin connect to the same MySQL database, `mirpur_advising_db`. The viewer signs in using the configured MySQL account and is accessible on this computer only. Refresh a table's Browse page after saving a change in the website. Redis and XAMPP are not needed by these launchers.

phpMyAdmin opens table Browse views in descending primary-key order, placing larger/newer IDs first. The shared configuration is mounted from `scripts/phpmyadmin/config.user.inc.php`, so the setting remains after restarting the viewer. A selected column can also be sorted manually; the default direction is descending.

All role pages read fresh data while visible, approximately once per second, and refresh when returning to the tab. Background updates preserve searches and unfinished forms. Database caching is disabled by default so external MySQL edits appear on these reads. Recent activity shows the latest eight logs with their database IDs; deleting older logs does not remove newer entries.

## Remaining features

- Admin creates, searches, activates/deactivates and deletes teacher/student accounts.
- Teacher Select saves a course and moves it to the upper selected list. Each course has its own Confirm/Remove actions. The four-course limit is checked on the server. Admin assignment protects assigned courses from being removed by the teacher.
- Teacher Requests assign teachers to sections. Course Offerings supports section editing, clickable Enabled/Disabled status, **Enrolled Students**, and Delete.
- Advising Control creates, opens, closes and deletes advising windows. Its manual cache/seat tool panel has been removed.
- Student Advising reserves seats for ten minutes, checks prerequisites, timetable clashes and credit limits, then confirms enrollment and updates the class routine.
- Semester Drop requires a reason, respects the deadline/minimum-credit rule, updates enrollment/routine and returns seats.
- Automatic caching, seat locks, expiry scheduling, role authorization and dashboard activity logs are retained because the remaining pages use them.

The schema contains 22 tables supporting these pages, authentication and dashboard activity. Student CGPA and prerequisite eligibility are retained for advising rules. `prerequisite_records` stores course eligibility without a separate grading workflow. Private database files and backups in `data/` are excluded from source control.

For an existing database created with an older schema, stop the website and run `scripts/migrate-core-schema.ps1` once before starting the updated package. The migration saves a full backup in `data/backups/`, retains prerequisite eligibility, and removes obsolete tables and columns. Fresh databases use the cleaned schema automatically.

## Build and verify

```powershell
.\mvnw.cmd clean test
node scripts/check-frontend.mjs
node scripts/check-oop-frontend.mjs
node scripts/check-workspace-pages.mjs
node scripts/check-course-selection.mjs
node scripts/check-account-deletion.mjs
node scripts/check-offering-status.mjs
node scripts/check-password-setup.mjs
node scripts/check-architecture.mjs
.\mvnw.cmd package -DskipTests
```

[OOP guide](docs/OOP-GUIDE.md) explains the remaining structure. [API contract](docs/api-contract.json) records the current controllers, pages and schema fingerprint.
