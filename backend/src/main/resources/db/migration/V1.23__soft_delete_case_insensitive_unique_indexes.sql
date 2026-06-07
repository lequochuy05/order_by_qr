-- Strengthen soft-delete unique indexes so application-level case-insensitive
-- uniqueness is also enforced by PostgreSQL.

DROP INDEX IF EXISTS uk_users_email_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_active
ON users (LOWER(email))
WHERE is_deleted = false;

DROP INDEX IF EXISTS uk_category_name_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_active
ON category (LOWER(name))
WHERE is_deleted = false;

DROP INDEX IF EXISTS uk_tables_table_number_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_number_active
ON tables (LOWER(table_number))
WHERE is_deleted = false;

DROP INDEX IF EXISTS uk_tables_table_code_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_code_active
ON tables (LOWER(table_code))
WHERE is_deleted = false;

DROP INDEX IF EXISTS uk_combos_name_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_combos_name_active
ON combos (LOWER(name))
WHERE is_deleted = false;

DROP INDEX IF EXISTS uk_vouchers_code_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_vouchers_code_active
ON vouchers (LOWER(code))
WHERE is_deleted = false;
