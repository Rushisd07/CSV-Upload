-- ============================================================
-- V1: Initial Schema - E-Commerce Data Model
-- Tables: customers, products, categories, orders, order_items
-- ============================================================

-- UPLOAD JOB TRACKING TABLE
CREATE TABLE IF NOT EXISTS upload_jobs (
    id              BIGSERIAL PRIMARY KEY,
    job_id          UUID NOT NULL UNIQUE,
    file_name       VARCHAR(500) NOT NULL,
    file_type       VARCHAR(10) NOT NULL,   -- CSV or JSON
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_rows      BIGINT DEFAULT 0,
    processed_rows  BIGINT DEFAULT 0,
    failed_rows     BIGINT DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id              BIGSERIAL PRIMARY KEY,
    category_code   VARCHAR(50) NOT NULL UNIQUE,
    category_name   VARCHAR(255) NOT NULL,
    description     TEXT,
    parent_id       BIGINT REFERENCES categories(id),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- CUSTOMERS TABLE
CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    customer_code   VARCHAR(100) NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(30),
    date_of_birth   DATE,
    country         VARCHAR(100),
    city            VARCHAR(100),
    address         TEXT,
    postal_code     VARCHAR(20),
    loyalty_points  INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL PRIMARY KEY,
    product_code    VARCHAR(100) NOT NULL UNIQUE,
    product_name    VARCHAR(500) NOT NULL,
    description     TEXT,
    category_id     BIGINT REFERENCES categories(id),
    unit_price      NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity  INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    weight_kg       NUMERIC(8, 3),
    brand           VARCHAR(255),
    sku             VARCHAR(100) UNIQUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(100) NOT NULL UNIQUE,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    shipping_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency        VARCHAR(10) NOT NULL DEFAULT 'USD',
    shipping_address TEXT,
    notes           TEXT,
    ordered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    shipped_at      TIMESTAMP,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ORDER ITEMS TABLE
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    discount        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    line_total      NUMERIC(15, 2) GENERATED ALWAYS AS ((quantity * unit_price) - discount) STORED,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES for performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_customers_email    ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_code     ON customers(customer_code);
CREATE INDEX IF NOT EXISTS idx_products_code      ON products(product_code);
CREATE INDEX IF NOT EXISTS idx_products_sku       ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_category  ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer    ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_number      ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order  ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_upload_jobs_job_id  ON upload_jobs(job_id);
CREATE INDEX IF NOT EXISTS idx_upload_jobs_status  ON upload_jobs(status);
