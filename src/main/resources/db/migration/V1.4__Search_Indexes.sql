-- Kích hoạt extension pg_trgm (yêu cầu quyền SUPERUSER hoặc CREATE EXTENSION)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Tối ưu Full-text search bằng GIN Index
CREATE INDEX idx_menu_item_name_gin ON menu_item USING gin (name gin_trgm_ops);
CREATE INDEX idx_category_name_gin ON category USING gin (name gin_trgm_ops);
CREATE INDEX idx_users_fullname_gin ON users USING gin (full_name gin_trgm_ops);
CREATE INDEX idx_users_email_gin ON users USING gin (email gin_trgm_ops);
CREATE INDEX idx_users_phone_gin ON users USING gin (phone gin_trgm_ops);