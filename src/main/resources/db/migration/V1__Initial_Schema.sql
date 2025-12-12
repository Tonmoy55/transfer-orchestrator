-- Create transfers table
CREATE TABLE transfers (
    id VARCHAR(255) PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    asset_id VARCHAR(255) NOT NULL,
    data_type VARCHAR(255),
    current_state VARCHAR(50) NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    retry_count INT DEFAULT 0,
    edc_transfer_process_id VARCHAR(255),
    edc_contract_agreement_id VARCHAR(255),
    consumer_region VARCHAR(100),
    consumer_certification_level VARCHAR(100),
    usage_purpose VARCHAR(100)
);

-- Create indexes for transfers
CREATE INDEX idx_transfers_state ON transfers(current_state);
CREATE INDEX idx_transfers_consumer ON transfers(consumer_id);
CREATE INDEX idx_transfers_created ON transfers(created_at);

-- Create audit_events table
CREATE TABLE audit_events (
    id VARCHAR(255) PRIMARY KEY,
    transfer_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    actor VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    details VARCHAR(2000),
    metadata VARCHAR(5000)
);

-- Create indexes for audit_events
CREATE INDEX idx_audit_transfer_id ON audit_events(transfer_id);
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp);

-- Create policies table
CREATE TABLE policies (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    configuration VARCHAR(2000),
    active BOOLEAN NOT NULL
);

-- Create rate_limit_tracking table
CREATE TABLE rate_limit_tracking (
    id VARCHAR(255) PRIMARY KEY,
    consumer_id VARCHAR(255) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    request_count INT NOT NULL,
    last_request TIMESTAMP NOT NULL
);

-- Create indexes for rate_limit_tracking
CREATE INDEX idx_rate_limit_consumer_window ON rate_limit_tracking(consumer_id, window_start);

-- Insert default policies
INSERT INTO policies (id, name, type, description, configuration, active) VALUES
('policy-1', 'Business Hours Only', 'TIME_BASED', 'Allow transfers only during business hours (8 AM - 6 PM)', '{"start":"08:00","end":"18:00"}', true),
('policy-2', 'Rate Limit 100/hour', 'RATE_LIMIT', 'Maximum 100 requests per hour per consumer', '{"maxRequests":100,"windowHours":1}', true),
('policy-3', 'EU Region Only', 'GEOGRAPHIC', 'Data must not leave EU region', '{"allowedRegions":["EU","EEA","GERMANY","FRANCE","ITALY"]}', true),
('policy-4', 'Quality Certification Required', 'CERTIFICATION', 'Consumer must have quality certification', '{"requiredCerts":["ISO9001","ISO27001","TISAX"]}', true),
('policy-5', 'Approved Usage Only', 'USAGE', 'Data usage must be for approved purposes', '{"allowedPurposes":["QUALITY_ANALYSIS","SUPPLY_CHAIN_OPTIMIZATION","COMPLIANCE_REPORTING"]}', true);

