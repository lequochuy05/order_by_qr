-- V1.20: Add ADMIN and CASHIER roles to the system
-- Upgrade existing MANAGER accounts to ADMIN as requested for restaurant owner role
UPDATE users
SET role = 'ADMIN'
WHERE role = 'MANAGER';
