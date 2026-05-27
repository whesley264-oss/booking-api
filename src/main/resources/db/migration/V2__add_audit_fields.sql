-- Add audit fields for compliance
ALTER TABLE bookings ADD COLUMN created_by VARCHAR(255);
ALTER TABLE bookings ADD COLUMN updated_by VARCHAR(255);
ALTER TABLE resources ADD COLUMN created_by VARCHAR(255);
ALTER TABLE resources ADD COLUMN updated_by VARCHAR(255);
