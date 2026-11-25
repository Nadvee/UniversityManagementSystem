SOURCE /tmp/mirpur-core-schema.sql;

-- Preserve the prerequisite eligibility needed by Student Advising.
SET @statement = IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='results'),
  'INSERT INTO prerequisite_records(id,student_id,course_id,semester_id,passed,recorded_at) SELECT id,student_id,course_id,semester_id,grade_point>=2,entered_at FROM results WHERE status=''PUBLISHED'' ON DUPLICATE KEY UPDATE passed=VALUES(passed),recorded_at=VALUES(recorded_at)',
  'SELECT 1');
PREPARE core_migration FROM @statement;
EXECUTE core_migration;
DEALLOCATE PREPARE core_migration;

-- Drop foreign-key children before their parents.
DROP TABLE IF EXISTS refund_records;
DROP TABLE IF EXISTS payment_items;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoice_items;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS published_assessment_marks;
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS section_assessments;
DROP TABLE IF EXISTS gradebooks;
DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS pre_advising_list;
DROP TABLE IF EXISTS user_permission_grants;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS grade_scale;
DROP TABLE IF EXISTS load_test_runs;

DELETE FROM audit_logs WHERE entity IN ('payment','invoice','invoice_item','refund','result','gradebook','permission','pre_advising','load_test')
  OR action REGEXP '^(PAYMENT_|REFUND_|INVOICE_|GRADE_|ASSESSMENT_|PERMISSION_|PRE_ADVISING_|LOAD_TEST_)';
DELETE FROM notices WHERE category IN ('PAYMENT','BILLING','GRADE','RESULT','PRE_ADVISING');

SET @statement = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='advising_rules' AND column_name='late_fee'),
  'ALTER TABLE advising_rules DROP COLUMN late_fee', 'SELECT 1');
PREPARE core_migration FROM @statement;
EXECUTE core_migration;
DEALLOCATE PREPARE core_migration;

SET @statement = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='advising_rules' AND column_name='late_fee_after_date'),
  'ALTER TABLE advising_rules DROP COLUMN late_fee_after_date', 'SELECT 1');
PREPARE core_migration FROM @statement;
EXECUTE core_migration;
DEALLOCATE PREPARE core_migration;

SET @statement = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sections' AND column_name='is_test'),
  'ALTER TABLE sections DROP COLUMN is_test', 'SELECT 1');
PREPARE core_migration FROM @statement;
EXECUTE core_migration;
DEALLOCATE PREPARE core_migration;

UPDATE advising_rules SET prerequisite_rules_text='Recorded prerequisite completion is required before selecting a course.';
