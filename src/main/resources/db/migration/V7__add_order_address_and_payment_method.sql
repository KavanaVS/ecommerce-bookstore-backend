ALTER TABLE orders
    ADD COLUMN shipping_line1 VARCHAR(255) NOT NULL,
    ADD COLUMN shipping_line2 VARCHAR(255),
    ADD COLUMN shipping_city VARCHAR(100) NOT NULL,
    ADD COLUMN shipping_state VARCHAR(100) NOT NULL,
    ADD COLUMN shipping_postal_code VARCHAR(20) NOT NULL,
    ADD COLUMN shipping_country VARCHAR(100) NOT NULL,
    ADD COLUMN billing_line1 VARCHAR(255) NOT NULL,
    ADD COLUMN billing_line2 VARCHAR(255),
    ADD COLUMN billing_city VARCHAR(100) NOT NULL,
    ADD COLUMN billing_state VARCHAR(100) NOT NULL,
    ADD COLUMN billing_postal_code VARCHAR(20) NOT NULL,
    ADD COLUMN billing_country VARCHAR(100) NOT NULL;

ALTER TABLE payments
    ADD COLUMN payment_method VARCHAR(20) NOT NULL;
