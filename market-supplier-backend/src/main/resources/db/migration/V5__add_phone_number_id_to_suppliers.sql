-- Add phone_number_id column to suppliers table
ALTER TABLE suppliers ADD COLUMN phone_number_id VARCHAR(50);

-- Add index for faster lookups
CREATE INDEX idx_suppliers_phone_number_id ON suppliers(phone_number_id);

-- Add comment
COMMENT ON COLUMN suppliers.phone_number_id IS 'WhatsApp Business API phone number ID for webhook routing';


