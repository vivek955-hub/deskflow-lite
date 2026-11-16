# Put DeskFlow Lite on GitHub

Do this from the `deskflow-lite` folder (the one that contains `README.md`).

## 1. Check it runs first

```bash
docker compose up --build
```

Open <http://localhost:3000> and log in with `admin@acme.test` / `Password@123`. Never push a project you have not run.

## 2. Create the repository

On GitHub: **New repository** → name it `deskflow-lite` → Public → do **not** tick "Add a README" → Create.

## 3. Push

```bash
git init
git branch -M main
git add .
git status          # check that .env, node_modules and target are NOT listed
git commit -m "Initial commit: multi-tenant helpdesk with AI triage"
git remote add origin https://github.com/vivek955-hub/deskflow-lite.git
git push -u origin main
```

## 4. Finish the repository page

- **About** (gear icon, top right): add the description "Multi-tenant helpdesk with AI-assisted ticket triage" and topics `spring-boot`, `java`, `react`, `mysql`, `jwt`, `multi-tenancy`, `docker`.
- **Actions** tab: confirm the CI run is green.
- **Pin** the repository on your profile.

## 5. Keep committing as you learn

A project with one giant commit looks copied. Each exercise in `docs/LEARN.md` is a real change: make it, then commit it with a clear message, for example:

```bash
git commit -m "Add ticket source column with Flyway migration"
git commit -m "Add endpoint for overdue tickets"
```

A history of small commits, written by you, is what shows an interviewer that you understand the code.
