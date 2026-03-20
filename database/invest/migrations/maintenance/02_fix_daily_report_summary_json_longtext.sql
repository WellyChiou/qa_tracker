USE invest;

ALTER TABLE daily_report
    MODIFY COLUMN summary_json LONGTEXT NOT NULL;
