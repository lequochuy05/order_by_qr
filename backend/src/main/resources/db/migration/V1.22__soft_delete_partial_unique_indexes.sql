-- V1.22__soft_delete_partial_unique_indexes.sql
-- Drop old unique constraints and replace them with partial unique indexes 
-- to support soft delete (is_deleted = false)

-- 1. Users table
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_active
ON users(email)
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_phone_active
ON users(phone)
WHERE phone IS NOT NULL AND is_deleted = false;

-- 2. Category table
ALTER TABLE category DROP CONSTRAINT IF EXISTS category_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_active
ON category(name)
WHERE is_deleted = false;

-- 3. Tables table
ALTER TABLE tables DROP CONSTRAINT IF EXISTS tables_table_number_key;
ALTER TABLE tables DROP CONSTRAINT IF EXISTS tables_table_code_key;
ALTER TABLE tables DROP CONSTRAINT IF EXISTS tables_qr_code_url_key;
ALTER TABLE tables DROP CONSTRAINT IF EXISTS tables_qr_code_public_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_number_active
ON tables(table_number)
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_table_code_active
ON tables(table_code)
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_qr_code_url_active
ON tables(qr_code_url)
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tables_qr_code_public_id_active
ON tables(qr_code_public_id)
WHERE is_deleted = false;

-- 4. Combos table
ALTER TABLE combos DROP CONSTRAINT IF EXISTS combos_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_combos_name_active
ON combos(name)
WHERE is_deleted = false;

-- 5. Vouchers table
ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS vouchers_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vouchers_code_active
ON vouchers(code)
WHERE is_deleted = false;
