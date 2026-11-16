-- DeskFlow Lite schema. Every business table carries tenant_id:
-- that single column is what keeps one company's data away from another's.

CREATE TABLE tenants (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(140) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
);

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
CREATE INDEX idx_users_tenant ON users (tenant_id);

CREATE TABLE tickets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    priority     VARCHAR(20)  NOT NULL,
    category     VARCHAR(30)  NOT NULL,
    requester_id BIGINT       NOT NULL,
    assignee_id  BIGINT       NULL,
    ai_triaged   BOOLEAN      NOT NULL DEFAULT FALSE,
    sla_breached BOOLEAN      NOT NULL DEFAULT FALSE,
    due_at       DATETIME(6)  NOT NULL,
    resolved_at  DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_tickets_tenant    FOREIGN KEY (tenant_id)    REFERENCES tenants (id),
    CONSTRAINT fk_tickets_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_assignee  FOREIGN KEY (assignee_id)  REFERENCES users (id)
);
-- The list screen always filters by tenant, usually by status, and sorts by newest.
CREATE INDEX idx_tickets_tenant_status  ON tickets (tenant_id, status);
CREATE INDEX idx_tickets_tenant_created ON tickets (tenant_id, created_at);
-- Used by the SLA job that looks for overdue open tickets.
CREATE INDEX idx_tickets_sla ON tickets (sla_breached, status, due_at);

CREATE TABLE comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    ticket_id   BIGINT      NOT NULL,
    author_id   BIGINT      NOT NULL,
    body        TEXT        NOT NULL,
    internal    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);
CREATE INDEX idx_comments_ticket ON comments (ticket_id);

CREATE TABLE ticket_events (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    ticket_id  BIGINT       NOT NULL,
    actor_id   BIGINT       NULL,
    type       VARCHAR(30)  NOT NULL,
    detail     VARCHAR(500) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_events_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_events_actor  FOREIGN KEY (actor_id)  REFERENCES users (id)
);
CREATE INDEX idx_events_ticket ON ticket_events (ticket_id);
