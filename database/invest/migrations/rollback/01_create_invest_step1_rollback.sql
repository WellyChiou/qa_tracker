-- Rollback for 20260318_01_create_invest_step1.sql

USE invest;

DROP TABLE IF EXISTS portfolio;
DROP TABLE IF EXISTS stock_price_daily;
DROP TABLE IF EXISTS stock;
