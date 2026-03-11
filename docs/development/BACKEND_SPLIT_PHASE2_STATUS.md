# Backend Split Phase 2 Status

## Goal

Make the current single backend codebase closer to:

- `personal-backend`
- `church-backend`

without changing business logic, API behavior, or database meaning.

This phase focuses on domain boundary cleanup inside one repository.

## Completed

### 1. Deployment boundary split

The project already supports dual backend containers:

- `backend-personal`
- `backend-church`

Related files:

- `docker-compose.yml`
- `nginx/nginx.conf`
- `nginx/nginx-https.conf`
- `nginx/nginx-http-only.conf`
- `scripts/deployment/deploy.sh`
- `scripts/monitoring/check-services.sh`
- `backend/src/main/java/com/example/helloworld/HelloWorldApplication.java`

### 2. Shared token blacklist

`TokenBlacklistService` was moved out of personal-only code into shared/common.

Used by:

- personal auth flow
- church auth flow
- JWT auth filter

### 3. Church JWT boundary cleanup

`ChurchJwtUtil` no longer depends on personal configuration.

It is now restricted to church token behavior only.

### 4. Church notification group dependency inversion

Church scheduler no longer directly uses personal `LineGroupRepository`.

Current structure:

- common interface: `service/common/ChurchNotificationGroupGateway.java`
- personal adapter: `service/personal/PersonalChurchNotificationGroupGateway.java`

### 5. LINE bot settings dependency inversion

Shared LINE config access no longer directly lives in common with personal implementation details.

Current structure:

- common interface: `service/common/LineBotSettingsGateway.java`
- personal adapter: `service/personal/PersonalLineBotSettingsGateway.java`

### 6. Church LINE business logic extraction

Church-specific LINE message handling was extracted from `LineBotService`.

Current structure:

- church domain logic: `service/church/ChurchLineMessageService.java`
- integration/orchestration remains in `service/line/LineBotService.java`

### 7. Personal line group persistence extraction

Personal line group persistence and member management were extracted from `LineBotService`.

Current structure:

- personal domain logic: `service/personal/PersonalLineGroupService.java`

### 8. Shared auth gateway

JWT filter no longer directly depends on personal/church auth implementations.

Current structure:

- `service/common/AuthDomain.java`
- `service/common/AuthGateway.java`
- `service/common/JwtTokenGateway.java`
- `service/personal/PersonalJwtTokenGateway.java`
- `service/church/ChurchJwtTokenGateway.java`

### 9. Shared authentication manager factory

Security config now uses shared authentication manager construction.

Current structure:

- `service/common/DomainAuthenticationManagerFactory.java`
- `config/SecurityConfig.java`
- `config/ChurchSecurityConfig.java`

### 10. Shared URL permission abstraction

URL permission filter no longer directly depends on personal/church permission entities or services.

Current structure:

- common interface: `service/common/UrlPermissionGateway.java`
- personal adapter: `service/personal/PersonalUrlPermissionGateway.java`
- church adapter: `service/church/ChurchUrlPermissionGateway.java`
- shared filter: `filter/UrlPermissionFilter.java`

## Current Boundary State

### Shared/common now mainly contains

- auth abstractions
- token blacklist
- permission abstractions
- notification target abstractions
- LINE settings abstractions

### Personal domain now mainly owns

- expense logic
- line group persistence
- personal auth/token generation
- personal permissions and menus
- personal system settings

### Church domain now mainly owns

- service schedule logic
- church auth/token generation
- church permissions and menus
- church content/admin logic
- church LINE message flow

## Remaining Blockers Before Splitting Into Two Repositories

### 1. `LineBotService` is still a mixed integration service

File:

- `backend/src/main/java/com/example/helloworld/service/line/LineBotService.java`

Current status:

- church logic has been reduced
- personal line-group persistence has been reduced
- but the service still orchestrates both personal and church flows

Recommendation:

- keep it as a temporary integration module in the single repo
- decide later whether it belongs to `personal-backend` or becomes a third integration module

### 2. App bootstrap is still dual-domain aware

File:

- `backend/src/main/java/com/example/helloworld/HelloWorldApplication.java`

Current status:

- startup logic already supports `APP_DOMAIN`
- but one application entry still knows both personal and church startup paths

Recommendation:

- acceptable for current dual-container phase
- split into separate application entrypoints when moving to dual repositories

### 3. JWT utilities still exist as separate concrete implementations

Files:

- `backend/src/main/java/com/example/helloworld/util/PersonalJwtUtil.java`
- `backend/src/main/java/com/example/helloworld/util/ChurchJwtUtil.java`

Current status:

- shared auth no longer depends on them directly
- but they still remain as concrete domain utilities

Recommendation:

- acceptable for current phase
- no urgent need to merge or redesign unless token rules must be unified

### 4. Shared adapters still depend on domain implementations

This is intentional in phase 2.

Examples:

- shared interface implemented by personal adapter
- shared interface implemented by church adapter

Current status:

- dependency direction is now correct
- but packaging is still in one repository

Recommendation:

- when splitting repos, move interfaces either to:
  - a small shared module
  - or duplicate minimal contracts if simpler

## Recommended Next Phase

### Option A: Stay in one repo, keep hardening boundaries

Do this if stability is the priority.

Tasks:

- reduce `LineBotService` further
- isolate startup/bootstrap per domain
- verify each domain can build independently from shared contracts

### Option B: Start physical project split

Do this only after environment/build is stable.

Target structure:

- `backend-personal/`
- `backend-church/`
- optional `backend-shared/`

Suggested order:

1. clone current backend into two project roots
2. keep shared contracts minimal
3. move personal-only code first
4. move church-only code second
5. leave integration code for last

## Verification Note

Structural cleanup has been implemented, but full Docker/Maven rebuild verification is currently blocked by external dependency download certificate failure:

- `PKIX path building failed`

This appears to be an environment/network certificate issue during Maven dependency resolution inside Docker, not a direct conclusion about the Java refactor itself.
