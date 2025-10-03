-- Add stock quantity to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_quantity INTEGER NOT NULL DEFAULT 0;

-- Optional: initialize nulls to zero just in case
UPDATE products SET stock_quantity = 0 WHERE stock_quantity IS NULL;

