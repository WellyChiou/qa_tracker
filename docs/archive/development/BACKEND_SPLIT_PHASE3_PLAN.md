# Backend Split Phase 3 Plan

## Goal

Turn the current single backend repository into a structure that can be physically split into:

- `backend-personal`
- `backend-church`

without changing business logic.

This phase defines:

- package ownership
- shared contract scope
- file move order
- application/config split strategy

## Scope

This document is for physical project split planning only.

Not in scope:

- UI redesign
- API behavior changes
- database schema redesign
- moving to microservices

## Target Project Shape

### Option 1. Recommended first step

Keep one mono-repo, but create two backend roots later:

- `backend-personal/`
- `backend-church/`

Optional later:

- `backend-shared/`

### Option 2. Avoid for now

Do not introduce a heavy shared library too early.

For the first real split, only keep a very small shared contract set.
If a contract is tiny and stable, sharing is acceptable.
If a contract is unstable, duplication is safer than premature abstraction.

## Ownership Matrix

### Personal project should own

Packages:

- `controller/personal`
- `service/personal`
- `scheduler/personal`
- `entity/personal`
- `repository/personal`
- `dto/common` for shared API wrapper only
- `util/PersonalJwtUtil`

Likely also belongs to personal:

- `service/line/LineBotService`
- `controller/invest`
- `service/invest`
- `entity/invest`
- `repository/invest`
- `dto/invest`
- `exception/invest`

Reason:

- current LINE bot orchestration is still closer to personal operational ownership
- invest module is not church-domain code

### Church project should own

Packages:

- `controller/church`
- `service/church`
- `scheduler/church`
- `entity/church`
- `repository/church`
- `util/church`
- `util/ChurchJwtUtil`
- `exception/church`
- `dto/church`

### Shared/minimal contracts should remain shared

Only keep these as true shared contracts if needed:

- `service/common/AuthDomain`
- `service/common/CommonUrlPermission`
- `service/common/NotificationTargetGroup`
- `service/common/JwtTokenGateway`
- `service/common/UrlPermissionGateway`
- `service/common/LineBotSettingsGateway`
- `service/common/ChurchNotificationGroupGateway`

Potentially shared infra:

- `service/common/TokenBlacklistService`
- `service/common/DomainAuthenticationManagerFactory`

Recommendation:

- keep interfaces and tiny records shared
- avoid moving big service implementations into shared

## Current Files That Should Stay Out Of Shared

These should not become a general shared library:

- `service/line/LineBotService`
- `util/PersonalJwtUtil`
- `util/ChurchJwtUtil`
- `service/personal/*`
- `service/church/*`

Reason:

- they encode domain behavior or domain-specific configuration access

## Physical Split Mapping

### `backend-personal`

Should contain:

- personal controllers
- invest controllers
- personal services
- invest services
- personal schedulers
- personal repositories and entities
- personal JWT utility
- personal LINE bot integration
- personal settings and permissions

Should temporarily include:

- shared interfaces copied from `service/common`
- shared blacklist service if still used by both

Resources:

- `application.properties` or `application-personal.properties`
- only primary datasource settings required by personal runtime
- optional church datasource settings removed unless still temporarily needed

### `backend-church`

Should contain:

- church controllers
- church services
- church schedulers
- church repositories and entities
- church JWT utility
- church admin/public content logic
- church check-in module

Should temporarily include:

- shared interfaces copied from `service/common`
- shared blacklist service if still used by both

Resources:

- `application.properties` or `application-church.properties`
- only church datasource settings required by church runtime
- primary datasource only retained if a temporary adapter still depends on personal-managed data

## Temporary Cross-Project Dependencies

These are the main items that may still require transitional handling during real split.

### 1. `LineBotService`

File:

- `backend/src/main/java/com/example/helloworld/service/line/LineBotService.java`

Decision:

- move to `backend-personal` first

Why:

- current orchestration still depends more on personal-side entities and services
- church logic has already been extracted out to `ChurchLineMessageService`

Follow-up:

- church project should call church-owned services directly
- if needed later, create a separate integration module

### 2. Church notification target groups

Current shape:

- shared interface in common
- implementation in personal

Decision:

- during first physical split, church project may temporarily depend on:
  - copied interface
  - adapter contract
  - external config or duplicated minimal lookup logic

Safer approach:

- duplicate the tiny contract first
- replace with API/config-based lookup later if needed

### 3. LINE bot settings

Current shape:

- interface in common
- implementation in personal

Decision:

- if church still reads personal-managed LINE config, duplicate the tiny interface first
- keep the implementation on the personal side until ownership changes

### 4. Token blacklist

Current shape:

- shared service in common

Decision:

- easiest first split is duplication with same behavior
- better long-term split is shared Redis/DB-backed blacklist if tokens must be revoked across both projects

## Application Entry Strategy

### Current state

One application entry:

- `backend/src/main/java/com/example/helloworld/HelloWorldApplication.java`

It is already partially domain-aware through `APP_DOMAIN`.

### Target state

Create two entrypoints later:

- `PersonalApplication`
- `ChurchApplication`

Recommended approach:

1. keep current bootstrap as reference
2. create personal-only bootstrap
3. create church-only bootstrap
4. move dynamic job initialization to domain-specific startup classes
5. remove `APP_DOMAIN` branching after split is stable

## Config Split Strategy

### Current config file

- `backend/src/main/resources/application.properties`

Current content includes:

- server settings
- personal datasource
- church datasource
- logging
- upload settings
- google sheets path

### Target config for personal

Should keep:

- server settings
- encoding/timezone
- personal datasource
- logging
- personal upload settings
- LINE/personal runtime settings still used by personal project

Should remove:

- church datasource once no longer needed

### Target config for church

Should keep:

- server settings
- encoding/timezone
- church datasource
- logging
- church upload path
- google sheets path

Should remove:

- personal datasource once transitional adapters are gone

## Docker Split Strategy

### Current state

Dual containers already exist from one codebase.

### Target state

Create:

- `backend-personal/Dockerfile`
- `backend-church/Dockerfile`

Compose target:

- `backend-personal` builds from `./backend-personal`
- `backend-church` builds from `./backend-church`

Nginx routing can remain:

- `/api/church/**` -> church backend
- other `/api/**` -> personal backend

## Recommended Execution Order

### Step 1. Freeze common contracts

Create and stabilize the smallest possible shared set:

- auth domain enum
- JWT gateway interface
- URL permission gateway interface
- target group record
- line settings gateway interface

### Step 2. Create physical skeletons

Create target folders:

- `backend-personal/`
- `backend-church/`

Copy first:

- `pom.xml`
- `Dockerfile`
- `src/main/resources`

### Step 3. Move personal-first code

Move:

- personal package tree
- invest package tree
- line integration module

Reason:

- personal side currently has more transitional ownership

### Step 4. Move church package tree

Move:

- church controllers/services/entities/repositories/schedulers/util

### Step 5. Recreate minimal shared contracts

Choose one:

- tiny shared module
- duplicate tiny interfaces in both projects

Recommendation for first split:

- duplicate tiny interfaces first

### Step 6. Split startup and config

Create:

- personal-only application entry
- church-only application entry
- personal-only properties
- church-only properties

### Step 7. Fix build and deploy paths

Update:

- `docker-compose.yml`
- local build scripts
- remote deployment scripts

Only after both projects build independently.

## Acceptance Criteria For Real Project Split

The split should be considered ready only when all are true:

1. `backend-personal` can build without church package code.
2. `backend-church` can build without personal package code.
3. shared contracts are minimal and stable.
4. nginx and deployment scripts point to separate build contexts.
5. login, refresh token, and protected API behavior remain unchanged.
6. scheduled jobs do not double-run across projects.

## Recommended Immediate Next Action

Do not move files yet.

First do one narrow implementation task:

- create a directory-level split manifest based on this document

That manifest should list, file by file, which target project each package tree belongs to.

After that, start the physical split with:

- `backend-personal` skeleton
- `backend-church` skeleton

## Verification Note

Current refactor status is structurally ready for split planning, but full Docker/Maven rebuild verification is still blocked by external certificate failure during dependency download:

- `PKIX path building failed`

That build-environment issue should be resolved before starting the physical repository split.
