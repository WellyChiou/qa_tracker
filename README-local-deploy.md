# Local Deployment Guide (Docker Compose)

## Scripts
### deploy_local.sh
Local deploy helper.

Usage:
- `./deploy_local.sh` (default = normal)
- `./deploy_local.sh normal`  
  For daily development (code changes only). No rebuild.
- `./deploy_local.sh build`  
  When dependencies changed (`package.json`, `package-lock.json`, `pom.xml`). Uses build cache.
- `./deploy_local.sh clean`  
  When Dockerfile/base image changed or cache is broken. Rebuild without cache.
- `./deploy_local.sh --help`

### verify_local.sh
Quick sanity check after bringing up containers.

Usage:
- `./verify_local.sh`

## Common Workflows
### Daily dev (most common)
```bash
./deploy_local.sh
./verify_local.sh
