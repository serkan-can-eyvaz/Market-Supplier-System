-- Add latitude and longitude coordinates to markets table
ALTER TABLE markets 
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

-- Add index for coordinate-based queries
CREATE INDEX idx_markets_coordinates ON markets (latitude, longitude);

-- Add comment for documentation
COMMENT ON COLUMN markets.latitude IS 'Market latitude coordinate for mapping';
COMMENT ON COLUMN markets.longitude IS 'Market longitude coordinate for mapping';
