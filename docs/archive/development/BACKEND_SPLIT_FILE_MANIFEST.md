# Backend Split File Manifest

## Purpose

This file is the execution manifest for the future physical split into:

- `backend-personal`
- `backend-church`

It translates the phase 3 plan into file-level move rules.

## Target Labels

- `PERSONAL`: move into `backend-personal`
- `CHURCH`: move into `backend-church`
- `SHARED`: keep as tiny shared contract or duplicate into both projects
- `TRANSITIONAL`: do not finalize ownership until the split is underway
- `IGNORE`: not part of Java application logic

## Directory Rules

### `PERSONAL`

Move all files under:

- `backend/src/main/java/com/example/helloworld/controller/personal/`
- `backend/src/main/java/com/example/helloworld/service/personal/`
- `backend/src/main/java/com/example/helloworld/entity/personal/`
- `backend/src/main/java/com/example/helloworld/repository/personal/`
- `backend/src/main/java/com/example/helloworld/scheduler/personal/`
- `backend/src/main/java/com/example/helloworld/controller/invest/`
- `backend/src/main/java/com/example/helloworld/service/invest/`
- `backend/src/main/java/com/example/helloworld/entity/invest/`
- `backend/src/main/java/com/example/helloworld/repository/invest/`
- `backend/src/main/java/com/example/helloworld/dto/invest/`
- `backend/src/main/java/com/example/helloworld/exception/invest/`

### `CHURCH`

Move all files under:

- `backend/src/main/java/com/example/helloworld/controller/church/`
- `backend/src/main/java/com/example/helloworld/service/church/`
- `backend/src/main/java/com/example/helloworld/entity/church/`
- `backend/src/main/java/com/example/helloworld/repository/church/`
- `backend/src/main/java/com/example/helloworld/scheduler/church/`
- `backend/src/main/java/com/example/helloworld/dto/church/`
- `backend/src/main/java/com/example/helloworld/exception/church/`
- `backend/src/main/java/com/example/helloworld/util/church/`

### `SHARED`

Shared or duplicated minimal contracts only:

- `backend/src/main/java/com/example/helloworld/dto/common/ApiResponse.java`
- `backend/src/main/java/com/example/helloworld/dto/common/PageResponse.java`
- `backend/src/main/java/com/example/helloworld/service/common/AuthDomain.java`
- `backend/src/main/java/com/example/helloworld/service/common/CommonUrlPermission.java`
- `backend/src/main/java/com/example/helloworld/service/common/NotificationTargetGroup.java`
- `backend/src/main/java/com/example/helloworld/service/common/JwtTokenGateway.java`
- `backend/src/main/java/com/example/helloworld/service/common/UrlPermissionGateway.java`
- `backend/src/main/java/com/example/helloworld/service/common/LineBotSettingsGateway.java`
- `backend/src/main/java/com/example/helloworld/service/common/ChurchNotificationGroupGateway.java`

### `SHARED` but may be duplicated first

- `backend/src/main/java/com/example/helloworld/service/common/TokenBlacklistService.java`
- `backend/src/main/java/com/example/helloworld/service/common/DomainAuthenticationManagerFactory.java`

## Single-File Ownership Decisions

### Controllers

- `backend/src/main/java/com/example/helloworld/controller/UploadController.java` -> `PERSONAL`

Reason:

- depends on personal configuration refresh
- public upload endpoint is currently personal-owned infrastructure

### Line integration

- `backend/src/main/java/com/example/helloworld/service/line/LineApiClient.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/service/line/LineBotService.java` -> `PERSONAL`

Reason:

- current orchestration is still mostly personal-owned
- church-specific logic has already been extracted into church service layer

### JWT utilities

- `backend/src/main/java/com/example/helloworld/util/PersonalJwtUtil.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/util/ChurchJwtUtil.java` -> `CHURCH`

### Config files

- `backend/src/main/java/com/example/helloworld/config/PrimaryDataSourceConfig.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/config/ChurchDataSourceConfig.java` -> `CHURCH`
- `backend/src/main/java/com/example/helloworld/config/ChurchEntityExcludeFilter.java` -> `CHURCH`
- `backend/src/main/java/com/example/helloworld/config/ChurchRepositoryExcludeFilter.java` -> `CHURCH`
- `backend/src/main/java/com/example/helloworld/config/PersonalLineBotConfig.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/config/ChurchLineBotConfig.java` -> `CHURCH`
- `backend/src/main/java/com/example/helloworld/config/SecurityConfig.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/config/ChurchSecurityConfig.java` -> `CHURCH`

### Config files duplicated into both

- `backend/src/main/java/com/example/helloworld/config/AsyncRetryConfig.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/config/EncodingConfig.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/config/MySQLPhysicalNamingStrategy.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/config/SchedulingConfig.java` -> `SHARED`

### Config files optional or backup

- `backend/src/main/java/com/example/helloworld/config/CorsConfig.java` -> `SHARED`

Reason:

- currently backup-only
- may be copied into both projects or removed if unused

### Application bootstrap

- `backend/src/main/java/com/example/helloworld/HelloWorldApplication.java` -> `TRANSITIONAL`

Target:

- replace with `PersonalApplication`
- replace with `ChurchApplication`

Do not move this file as-is into either final project.

## Resource Files

### `TRANSITIONAL`

- `backend/src/main/resources/application.properties`

Target replacement:

- `backend-personal/src/main/resources/application.properties`
- `backend-church/src/main/resources/application.properties`

### Personal resource ownership

Keep in personal config:

- primary datasource settings
- personal upload settings
- personal logging defaults
- any personal runtime values still used by line/invest modules

### Church resource ownership

Keep in church config:

- church datasource settings
- church upload path
- google sheets path
- church logging defaults

## Shared Adapter Mapping

These are currently adapter pairs and should be handled intentionally during split.

### JWT adapter pair

- `backend/src/main/java/com/example/helloworld/service/common/JwtTokenGateway.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/service/personal/PersonalJwtTokenGateway.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/service/church/ChurchJwtTokenGateway.java` -> `CHURCH`

### URL permission adapter pair

- `backend/src/main/java/com/example/helloworld/service/common/UrlPermissionGateway.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/service/personal/PersonalUrlPermissionGateway.java` -> `PERSONAL`
- `backend/src/main/java/com/example/helloworld/service/church/ChurchUrlPermissionGateway.java` -> `CHURCH`

### Notification group adapter pair

- `backend/src/main/java/com/example/helloworld/service/common/ChurchNotificationGroupGateway.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/service/personal/PersonalChurchNotificationGroupGateway.java` -> `PERSONAL`

### LINE settings adapter pair

- `backend/src/main/java/com/example/helloworld/service/common/LineBotSettingsGateway.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/service/personal/PersonalLineBotSettingsGateway.java` -> `PERSONAL`

## Auth/Filter Mapping

### Shared or duplicate into both

- `backend/src/main/java/com/example/helloworld/filter/JwtAuthenticationFilter.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/filter/UrlPermissionFilter.java` -> `SHARED`
- `backend/src/main/java/com/example/helloworld/service/common/AuthGateway.java` -> `SHARED`

Recommendation:

- first physical split can duplicate these files into both projects
- do not block the split on building a separate shared module

## Files To Ignore During Split

- `backend/src/main/java/com/example/helloworld/service/.DS_Store`
- `backend/src/main/java/com/example/helloworld/dto/.DS_Store`
- `backend/src/main/java/com/example/helloworld/scheduler/.DS_Store`

## Execution Order

### Batch 1. Safe package moves

Move first:

- all `controller/personal`
- all `service/personal`
- all `entity/personal`
- all `repository/personal`
- all `scheduler/personal`
- all `controller/church`
- all `service/church`
- all `entity/church`
- all `repository/church`
- all `scheduler/church`
- all `dto/church`
- all `exception/church`

### Batch 2. Single-owner files

Move next:

- `controller/UploadController.java`
- `service/line/LineApiClient.java`
- `service/line/LineBotService.java`
- `util/PersonalJwtUtil.java`
- `util/ChurchJwtUtil.java`
- personal-only config files
- church-only config files

### Batch 3. Shared/duplicate files

Copy or duplicate next:

- `dto/common/*`
- `service/common/*` minimal contracts
- filters
- auth factory

### Batch 4. Bootstrap replacement

Create new entrypoints:

- `backend-personal/src/main/java/.../PersonalApplication.java`
- `backend-church/src/main/java/.../ChurchApplication.java`

Then remove dependency on `HelloWorldApplication.java`.

### Batch 5. Resource split

Create:

- personal `application.properties`
- church `application.properties`

Then remove unused datasource/settings from each side.

## Acceptance Check For This Manifest

This manifest is considered good enough to execute when:

1. every file is covered by a directory rule or single-file decision
2. shared files are intentionally minimal
3. `HelloWorldApplication.java` is treated as transitional, not final
4. `LineBotService.java` is explicitly assigned to personal for first split

## Notes

- This manifest intentionally favors fast physical separation over perfect abstraction purity.
- If a shared interface is tiny, duplication is acceptable in the first split.
- Do not delay the split by trying to invent a large `backend-shared` library.
