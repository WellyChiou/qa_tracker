# Project Refresh Status

## Current Architecture

This repository is currently organized as:

- `frontend-personal`: personal member-facing site
- `frontend-church`: public church-facing site
- `frontend-church-admin`: church admin backend UI
- `backend`: shared Java backend API
- `nginx` + `mysql`: deployment infrastructure

The old `frontend-invest` folder has been removed from the active project structure and documentation.

## What Was Cleaned Up

### Backend

- Removed church backend endpoints that were not referenced by the current public/admin frontends.
- Fixed the `service-schedules` route mismatch so frontend calls now align with backend routes.
- Removed dead service methods created by the controller cleanup.
- Removed the unused `CsvService`.

### Frontend API Layer

- Unified the frontend API helper contract so request helpers now consistently return parsed data instead of mixing parsed payloads and raw `Response` objects.
- Extracted shared API client logic into `shared/utils/apiClient.js`.
- Extracted shared church/church-admin API module wiring into `shared/utils/churchApiModule.js`.
- Extracted shared church scheduled-jobs wrappers into `shared/utils/churchScheduledJobs.js`.
- Fixed church/church-admin refresh-token parsing so `ApiResponse.data.accessToken` is handled correctly.

### Docs

Updated these files to reflect the real 3-frontend + 1-backend structure:

- `README.md`
- `README-local-deploy.md`
- `docs/CHURCH_SYSTEM_SETUP.md`
- `docs/deployment/QUICK_START.md`

## Frontend Refresh Progress

### `frontend-personal`

Refreshed:

- top navigation
- login page
- dashboard
- shared visual tokens
- QA tracker summary/count behavior fix
- QA tracker copy-to-clipboard action for in-progress records

### `frontend-church`

Refreshed:

- app shell
- home page
- about page
- activities page

### `frontend-church-admin`

Refreshed:

- login
- dashboard
- announcements
- persons
- activities
- groups
- roles
- service schedule
- permissions
- URL permissions
- menus
- users
- scheduled jobs
- church info
- about info
- sunday messages
- contact submissions
- maintenance
- attendance rate
- check-in session list
- check-in session detail
- manual check-in audit

## UI Direction

The admin/frontend refresh so far follows these principles:

- clearer page headers with page descriptions
- overview summary cards before dense tables
- card-based content containers instead of flat sections
- improved visual hierarchy for filters, tables, and empty states
- keep existing API/business flow stable while modernizing presentation

## Validation Status

Static inspection has been completed on the changed files.

Build verification completed successfully on March 11, 2026:

- `frontend-personal`: `npm run build`
- `frontend-church`: `npm run build`
- `frontend-church-admin`: `npm run build`
- `backend`: `mvn -q -DskipTests compile`

Not yet completed:

- backend automated tests
- any browser-based manual regression pass across the refreshed pages

## Recommended Next Steps

1. Run backend automated tests when the intended test command/scope is confirmed.
2. Do a manual regression pass on the refreshed login, dashboard, list, and modal flows.
3. Do a focused browser pass on the personal QA tracker flows, especially yearly stats, filtered counts, and clipboard export output.
4. Continue the next UI pass on shared modal/form/table consistency.
5. Consider extracting shared design tokens/components across the three frontend apps.
