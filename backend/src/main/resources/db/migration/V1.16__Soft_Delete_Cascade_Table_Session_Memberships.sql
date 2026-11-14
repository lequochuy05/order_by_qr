-- V1.16__Soft_Delete_Cascade_Table_Session_Memberships.sql
-- Adds a trigger to automatically soft-delete table_session_tables
-- when the parent table_sessions is soft-deleted.

CREATE OR REPLACE FUNCTION soft_delete_session_memberships()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE table_session_tables
    SET is_deleted = true, version = version + 1, updated_at = NOW()
    WHERE session_id = OLD.id AND is_deleted = false;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_table_sessions_soft_delete_memberships
    AFTER UPDATE OF is_deleted ON table_sessions
    FOR EACH ROW
    WHEN (NEW.is_deleted = true AND OLD.is_deleted = false)
    EXECUTE FUNCTION soft_delete_session_memberships();
