# Invest Database

## Directory layout

- `infra/`
  - DBA/init level scripts (database + grants only)
- `migrations/schema/`
  - schema/table/index/constraint only
- `migrations/seed-baseline/`
  - required baseline data (ACL/admin/risk rules)
- `migrations/seed-demo/`
  - optional sample/demo data
- `migrations/maintenance/`
  - optional maintenance repair scripts (legacy environments)
- `migrations/rollback/`
  - rollback scripts for manual operations

## Apply order (from 0)

1. `infra/00_create_invest_db_and_user.sql`
2. `migrations/schema/01_create_invest_step1.sql`
3. `migrations/schema/02_create_invest_step2_risk.sql`
4. `migrations/schema/03_create_invest_acl.sql`
5. `migrations/schema/04_create_invest_step3_report.sql`
6. `migrations/schema/05_create_invest_step4_alert.sql`
7. `migrations/schema/06_create_invest_v2_phase1_price_update.sql`
8. `migrations/schema/07_create_invest_v2_phase1_2_price_update.sql`
9. `migrations/seed-baseline/01_seed_risk_rules.sql`
10. `migrations/seed-baseline/02_seed_acl.sql`
11. `migrations/seed-baseline/03_hide_invest_risk_results_menu.sql`
12. `migrations/seed-baseline/04_set_admin_password_baseline.sql`
13. `migrations/seed-baseline/05_seed_system_menu_permission.sql`
14. `migrations/seed-baseline/06_seed_step3_report_acl.sql`
15. `migrations/seed-baseline/07_seed_step4_alert_acl.sql`
16. `migrations/seed-baseline/08_seed_v2_phase1_price_update_acl.sql`
17. (optional) `migrations/seed-demo/01_seed_step1_demo_data.sql`

## Notes

- App migrations must not contain `CREATE DATABASE` or `GRANT`.
- Demo seed is optional and should not be enabled in production by default.
- Baseline admin password is only for initialization verification and should be rotated after first deployment.
