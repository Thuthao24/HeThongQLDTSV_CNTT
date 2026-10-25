-- REVIEW ONLY: not executed. All writes are qualified to quanlydetaisinhvien_demo.
-- Use a NEW dedicated connection with no pending transaction; DDL implicitly commits.
-- Workbench: stop on first error. Do not run 00-05 as one batch (04 is a RESET).
CREATE DATABASE IF NOT EXISTS quanlydetaisinhvien_demo
 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quanlydetaisinhvien_demo;
SELECT DATABASE() AS Target_database;
