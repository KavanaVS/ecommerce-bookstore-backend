-- Seed data for local/dev environments.
-- Admin password is "Admin@12345" (BCrypt-hashed) — see README for details.

INSERT INTO categories (name, description) VALUES
    ('Fiction', 'Novels and short story collections'),
    ('Non-Fiction', 'Biographies, essays, and real-world narratives'),
    ('Science', 'Books on science, mathematics, and technology');

INSERT INTO books (title, author, isbn, price, stock_quantity, category_id, description) VALUES
    ('Pride and Prejudice', 'Jane Austen', '9780141439518', 9.99, 25, 1, 'A classic novel of manners set in Georgian England.'),
    ('Moby-Dick', 'Herman Melville', '9781503280786', 11.50, 15, 1, 'The voyage of the whaling ship Pequod and Captain Ahab''s obsession.'),
    ('1984', 'George Orwell', '9780451524935', 10.99, 30, 1, 'A dystopian vision of a totalitarian future.'),
    ('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 8.99, 20, 1, 'A story of wealth, love, and the American Dream in the Jazz Age.'),
    ('Long Walk to Freedom', 'Nelson Mandela', '9780316548182', 14.99, 12, 2, 'The autobiography of Nelson Mandela.'),
    ('Sapiens: A Brief History of Humankind', 'Yuval Noah Harari', '9780062316097', 16.99, 18, 2, 'A survey of the history of the human species.'),
    ('Into Thin Air', 'Jon Krakauer', '9780385494786', 13.50, 10, 2, 'A first-hand account of the 1996 Everest disaster.'),
    ('A Brief History of Time', 'Stephen Hawking', '9780553380163', 15.99, 20, 3, 'An exploration of cosmology for a general audience.'),
    ('The Selfish Gene', 'Richard Dawkins', '9780198788607', 12.99, 16, 3, 'A gene-centred view of evolution.'),
    ('The Code Book', 'Simon Singh', '9780385495318', 13.99, 14, 3, 'The science of secrecy from ancient Egypt to quantum cryptography.');

INSERT INTO users (name, email, password_hash, role) VALUES
    ('Admin', 'admin@bookstore.com', '$2a$10$hNvhUGOql5Rafmt.3edZXOk0400TLhjod7/ZgkSYKXakBDOMf1Ny6', 'ADMIN');
