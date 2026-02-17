-- Database: project_db

CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    lead_id UUID,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')) DEFAULT 'DRAFT'
);

CREATE TABLE IF NOT EXISTS client_briefs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    objective TEXT,
    target_audience VARCHAR(255),
    video_type VARCHAR(100),
    requirements JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS storyboards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    version INT DEFAULT 1,
    scenes JSONB,
    ai_model_version VARCHAR(50),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    due_date DATE,
    status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
