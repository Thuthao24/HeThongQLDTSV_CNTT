-- REVIEW ONLY: not executed. All writes are qualified to quanlydetaisinhvien_demo.
-- Use a NEW dedicated connection with no pending transaction; DDL implicitly commits.
-- Workbench: stop on first error. Do not run 00-05 as one batch (04 is a RESET).
-- First select the DEMO schema and set @hcmute_demo_confirmation manually (see README).
SELECT DATABASE() AS Target_database;
CALL quanlydetaisinhvien_demo.hn_reset_demo_v2(DATABASE(), 'INIT', @hcmute_demo_confirmation);
SET @hcmute_demo_confirmation = NULL;
