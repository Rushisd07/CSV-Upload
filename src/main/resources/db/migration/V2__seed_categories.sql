-- ============================================================
-- V2: Seed Data - Categories and sample reference data
-- ============================================================

INSERT INTO categories (category_code, category_name, description, parent_id, is_active) VALUES
('ELECTRONICS',   'Electronics',       'Electronic devices and accessories', NULL, TRUE),
('CLOTHING',      'Clothing',          'Apparel and fashion items',          NULL, TRUE),
('BOOKS',         'Books',             'Books and educational materials',    NULL, TRUE),
('HOME',          'Home & Garden',     'Home improvement and garden',        NULL, TRUE),
('SPORTS',        'Sports & Outdoors', 'Sports equipment and outdoor gear',  NULL, TRUE),
('LAPTOPS',       'Laptops',           'Portable computers',                 1, TRUE),
('PHONES',        'Smartphones',       'Mobile phones and accessories',      1, TRUE),
('TABLETS',       'Tablets',           'Tablet computers',                   1, TRUE),
('MENS',          'Men''s Clothing',   'Clothing for men',                   2, TRUE),
('WOMENS',        'Women''s Clothing', 'Clothing for women',                 2, TRUE),
('FICTION',       'Fiction',           'Novels and fiction books',           3, TRUE),
('NONFICTION',    'Non-Fiction',       'Educational and non-fiction',        3, TRUE),
('FURNITURE',     'Furniture',         'Home furniture',                     4, TRUE),
('KITCHENWARE',   'Kitchenware',       'Kitchen tools and appliances',       4, TRUE),
('FITNESS',       'Fitness',           'Fitness equipment and accessories',  5, TRUE)
ON CONFLICT (category_code) DO NOTHING;
