-- =============================================
-- SKILORA - MISSING TABLES FIX
-- Run this if you get "Table 'skilora.companies' doesn't exist"
-- =============================================

USE skilora;

-- Companies table (needed for job_offers and external job feed)
CREATE TABLE IF NOT EXISTS companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50),
    industry VARCHAR(100),
    website VARCHAR(255),
    logo_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    size VARCHAR(50),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Job Offers table (needed for job feed)
CREATE TABLE IF NOT EXISTS job_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    requirements TEXT,
    min_salary DECIMAL(10,2),
    max_salary DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'EUR',
    location VARCHAR(100),
    work_type VARCHAR(50),
    posted_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    deadline DATE,
    status VARCHAR(20) DEFAULT 'OPEN',
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Verify tables were created
SELECT 'Tables created successfully!' AS status;
SHOW TABLES LIKE 'companies';
SHOW TABLES LIKE 'job_offers';
