# Learn DeskFlow Lite, phase by phase

The goal: you can open any file in this project and explain what it does and why it is written that way.
Do the phases in order. For each one, read the files, do the exercise, and answer the questions out loud without looking.

A rule for interviews: only claim what you can explain. If you finish phases 1 to 4, you can honestly defend the backend. Phases 5 to 7 cover the rest.

---

## Phase 1: Run it and follow one request (day 1)

**Do:** Start the app (see README). Log in as `agent@acme.test`. Open the browser's Network tab and click a ticket.

**Read, in this order:**

1. `frontend/src/api.js` - how the browser sends the request and attaches the token
2. `security/JwtAuthFilter.java` - the token is checked before anything else runs
3. `ticket/TicketController.java` - the `get` method
4. `ticket/TicketService.java` - `get`, `loadVisible`, `toDetail`
5. `ticket/TicketRepository.java` - `findByIdAndTenantId`
6. `resources/db/migration/V1__init.sql` - the table the data comes from

**Exercise:** Draw the path of `GET /api/tickets/1` on paper: browser → filter → controller → service → repository → MySQL → back.

**You should be able to answer:**

- What does each layer (controller, service, repository) do, and why split them?
- What is a DTO, and why does `TicketDetail` exist when there is already a `Ticket` entity?
- What is dependency injection? Where does `TicketService` get its `TicketRepository` from?

## Phase 2: Database and JPA (day 2)

**Read:** `V1__init.sql`, `ticket/Ticket.java`, `user/User.java`, all `*Repository.java` files.

**Exercise:** Add a new migration `V2__add_ticket_source.sql` that adds a `source VARCHAR(20)` column to tickets (values like `WEB`, `EMAIL`), add the field to `Ticket`, and show it in the API.

**You should be able to answer:**

- What does Flyway do? Why is `ddl-auto=none` safer than `update`?
- What do `@ManyToOne` and `FetchType.LAZY` mean?
- What is the N+1 query problem, and how does `@EntityGraph` in `TicketRepository` avoid it?
- Why are there indexes on `(tenant_id, status)` and `(tenant_id, created_at)`?
- How does Spring turn the method name `findByIdAndTenantId` into SQL?
- What does `@Transactional` do? In `TicketService.update`, nothing calls `save()`. Why is the change still stored? (Search: "JPA dirty checking".)

## Phase 3: Security and JWT (day 3)

**Read:** `security/SecurityConfig.java`, `JwtService.java`, `JwtAuthFilter.java`, `CurrentUser.java`, `auth/AuthService.java`.

**Exercise:** Log in through Swagger, copy the token, and paste it into <https://jwt.io>. Find the user id, tenant id and role inside it. Then change one character of the token and call an API: see the 401.

**You should be able to answer:**

- What are the three parts of a JWT? Can the user read it? Can the user change it? Why not?
- Why is the password stored with BCrypt and not plain or with SHA-256?
- What does "stateless" mean, and why does it help when running several copies of the server?
- Why is CSRF protection switched off here, and when would that be wrong?
- What is CORS, and why is it needed in development but not in the Docker setup?
- What is the difference between 401 and 403?
- Why does login give the same error for a wrong email and a wrong password?

## Phase 4: Multi-tenancy and authorisation (day 4)

This is the headline feature. Know it cold.

**Read:** `ticket/TicketService.java` from top to bottom, `user/UserService.java`, `seed/DemoDataSeeder.java`.

**Exercise:** Log in as `admin@acme.test`, note a ticket id. Log in as `admin@globex.test` and call `GET /api/tickets/{that id}` in Swagger. You get 404. Find the exact line of code that caused it.

**You should be able to answer:**

- What is multi-tenancy? Name the three common designs (shared tables with a tenant column, schema per tenant, database per tenant) and one advantage and one risk of each.
- Why did this project choose a shared table with `tenant_id`?
- Where does the tenant id come from on each request? Why must it never come from the request body or URL?
- A customer sends `GET /api/tickets?mine=false`. Why do they still only see their own tickets?
- Why does the app answer 404 and not 403 for another company's ticket?
- How does `@PreAuthorize("hasAnyRole('ADMIN','AGENT')")` work?
- What is the biggest weakness of this design, and how could you make it safer? (One answer: Hibernate's `@TenantId` or a filter applied automatically, so a developer cannot forget the tenant condition.)

## Phase 5: The AI features (day 5)

**Read:** `ai/AiService.java`, `RuleBasedAi.java`, `LlmClient.java`, `SmartAiService.java`, `SimilarityService.java`, and their tests.

**Exercise:** Get a free API key from an OpenAI-compatible provider, set `AI_API_KEY`, restart, and create a ticket. The chip in the UI changes from "AI rules" to "AI model". Then set a wrong key and create another ticket: it still works. Read the log line that explains why.

**You should be able to answer:**

- Why is there an interface with two implementations? (Which design patterns is this? Strategy, plus a fallback.)
- What happens if the LLM returns a category that does not exist? Follow the code.
- What is prompt injection? Which line in the prompts is there to reduce it, and why is checking the answer against the enum the real protection?
- Explain Jaccard similarity with a small example. What are its weaknesses? (It misses synonyms: "login" vs "sign in".)
- What are embeddings, and how would they improve duplicate detection?
- Why do the AI calls have short timeouts?

## Phase 6: SLA job, dashboard, error handling (day 6)

**Read:** `ticket/SlaScheduler.java`, `ticket/Priority.java`, `dashboard/DashboardService.java`, `common/GlobalExceptionHandler.java`.

**Exercise:** Add an endpoint `GET /api/tickets/overdue` for staff that returns the tenant's SLA-breached active tickets.

**You should be able to answer:**

- What does `@Scheduled` do? What goes wrong if two copies of the app run the same job, and how would you fix it? (Search: "ShedLock".)
- The dashboard loads all tickets and counts in Java. When does that stop being acceptable, and what would you write instead?
- What does `@RestControllerAdvice` do? Why is one error format useful for the frontend?
- How does `@Valid` on a request body work, and where do the messages come from?

## Phase 7: Frontend, Docker, CI (day 7)

**Read:** `frontend/src/auth.jsx`, `App.jsx`, `pages/Tickets.jsx`, both `Dockerfile`s, `docker-compose.yml`, `frontend/nginx.conf`, `.github/workflows/ci.yml`.

**Exercise:** Push to GitHub and watch the CI run pass. Then break a test on purpose, push, and watch it fail.

**You should be able to answer:**

- What do `useState`, `useEffect` and React Context do in `auth.jsx`?
- Where is the token stored in the browser, and what is the risk? (XSS can read `localStorage`. The alternative is an HttpOnly cookie.)
- What is a multi-stage Docker build, and why is the final image smaller?
- In `docker-compose.yml`, why does the backend use `db:3306` and not `localhost:3306`?
- What does `depends_on: condition: service_healthy` solve?
- What does the CI workflow check on every push?

---

## Your 60-second project pitch

Practise saying this, in your own words:

> DeskFlow Lite is a multi-tenant helpdesk. Several companies share one Spring Boot application and one MySQL database, and each company's data is isolated with a tenant id that comes from the signed JWT, never from the request. It has three roles, a full ticket lifecycle with SLA deadlines checked by a scheduled job, and an audit history. New tickets are triaged automatically: an AI layer sets the category and priority, drafts replies, summarises threads and finds likely duplicates. The AI sits behind an interface with an LLM engine and a rule-based engine, so if the LLM is unavailable the app falls back and nothing breaks. The schema is managed with Flyway, the API is documented with Swagger, and the whole thing runs with one Docker Compose command and is checked by GitHub Actions.

## Resume bullets (use only what you can explain)

- Built a multi-tenant helpdesk with Spring Boot 3, MySQL and React, isolating each company's data through a tenant id carried in a signed JWT.
- Implemented role-based access control (admin, agent, customer) with Spring Security, stateless JWT authentication and BCrypt password hashing.
- Added AI ticket triage, reply drafting and summarisation behind a single interface, with an LLM engine and a rule-based fallback so the feature degrades gracefully.
- Implemented duplicate-ticket detection using Jaccard similarity, and SLA tracking with a scheduled background job and an audit trail of every change.
- Versioned the database schema with Flyway, documented the REST API with OpenAPI, containerised the stack with Docker Compose and added a GitHub Actions CI pipeline.
