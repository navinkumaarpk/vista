# VISTA — Setup, Run, Test & Teardown Guide

This guide walks through everything needed to get VISTA (Virtual Infrastructure Surveillance and Triage Agent) running from a clean machine: installing dependencies, starting background services, seeding mock data, running through test scenarios, and shutting everything down cleanly.

It covers **Windows** and **Ubuntu** (WSL2 or native) side by side. Pick the column that matches your environment at each step.

**Repository:** https://github.com/navinkumaarpk/vista

---

## 1. Setting Up Libraries, Environment, and Tools

### 1.1 Clone the repository

```bash
git clone https://github.com/navinkumaarpk/vista.git
cd vista/vista
```

(Both Windows PowerShell/Git Bash and Ubuntu use the same command.)

### 1.2 Core prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java (JDK) | 21 | Runtime for the Spring Boot application |
| Maven | bundled (`mvnw` wrapper included) | Build and run the project |
| PostgreSQL | 16+ | Vector store (pgvector) for RAG |
| pgvector extension | 0.8.0+ | Vector similarity search inside Postgres |
| MongoDB | 8.0 | Telemetry / stats storage |
| Ollama | latest | Runs local models (gpt-oss, gemma, embeddings) |
| Anthropic API key | — | Optional, only needed to use Claude as a model |

#### Windows

1. **Java 21** — install via [Adoptium Temurin 21](https://adoptium.net/) (MSI installer). Verify:
   ```powershell
   java -version
   ```
2. **PostgreSQL** — install via the [EnterpriseDB Windows installer](https://www.postgresql.org/download/windows/). During setup, note the password you set for the `postgres` user. Add `pgvector` afterward using [Stack Builder](https://www.postgresql.org/download/windows/) (bundled with the installer) or build from source if it isn't offered for your version.
3. **MongoDB** — install via the [MongoDB Community MSI installer](https://www.mongodb.com/try/download/community). Choose "Install as a Service" during setup so it can be controlled via Windows Services.
4. **Ollama** — install via the [Ollama Windows installer](https://ollama.com/download/windows). It runs as a background service automatically after install.
5. **Git** — install via [git-scm.com](https://git-scm.com/download/win) if not already present.

#### Ubuntu (24.04)

```bash
# Java 21
sudo apt update
sudo apt install -y openjdk-21-jdk

# PostgreSQL + pgvector
sudo apt install -y postgresql postgresql-contrib
sudo apt install -y postgresql-16-pgvector
# If that package isn't found for your Postgres version, build from source:
#   sudo apt install -y build-essential postgresql-server-dev-16 git
#   git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git
#   cd pgvector && make && sudo make install && cd ..

# MongoDB 8.0
sudo apt-get install -y gnupg curl
curl -fsSL https://www.mongodb.org/static/pgp/server-8.0.asc | \
  sudo gpg -o /usr/share/keyrings/mongodb-server-8.0.gpg --dearmor
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-8.0.gpg ] https://repo.mongodb.org/apt/ubuntu noble/mongodb-org/8.0 multiverse" | \
  sudo tee /etc/apt/sources.list.d/mongodb-org-8.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org

# Ollama
curl -fsSL https://ollama.com/install.sh | sh
```

> **Note for WSL2 users:** WSL2 typically does not run systemd by default, so services are started manually (`sudo service ...` or direct binaries) rather than via `systemctl`. Commands below account for this.

### 1.3 Pull the required models (both OSes)

```bash
ollama pull gpt-oss:20b
ollama pull gemma4:12b
ollama pull nomic-embed-text
```

This step is identical on Windows and Ubuntu since it just talks to the local Ollama service. Pulling `gpt-oss:20b` and `gemma4:12b` requires roughly 16GB of free disk space combined and a GPU with at least 12–16GB VRAM is recommended for reasonable performance; CPU-only inference will work but is slow.

### 1.4 Set the Anthropic API key (optional)

Only required if you want to use Claude as a selectable model.

**Windows (PowerShell):**
```powershell
setx ANTHROPIC_API_KEY "sk-ant-..."
```
(Restart your terminal afterward for it to take effect.)

**Ubuntu:**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
echo 'export ANTHROPIC_API_KEY="sk-ant-..."' >> ~/.bashrc
```

### 1.5 Compile the project

From the `vista/vista` directory (both OSes use the included Maven wrapper, no separate Maven install needed):

**Windows:**
```powershell
.\mvnw.cmd compile
```

**Ubuntu:**
```bash
./mvnw compile
```

A `BUILD SUCCESS` message confirms the project compiles cleanly.

---

## 2. Starting Background Services & Seeding Mock Data

VISTA depends on three background services being up before the application starts: **PostgreSQL**, **MongoDB**, and **Ollama**.

### 2.1 Start the services

#### Windows

PostgreSQL and MongoDB run as Windows Services if installed with the defaults, so they typically start automatically on boot. To start them manually:

```powershell
net start postgresql-x64-16
net start MongoDB
```

Ollama runs as a background app after installation; if it isn't running, launch it from the Start Menu, or run:
```powershell
ollama serve
```

#### Ubuntu

```bash
# PostgreSQL
sudo service postgresql start

# MongoDB (no systemd in most WSL2 setups, so fork manually)
sudo mkdir -p /var/lib/mongodb /var/log/mongodb
sudo chown -R mongodb:mongodb /var/lib/mongodb /var/log/mongodb
sudo -u mongodb mongod --config /etc/mongod.conf --fork

# Ollama (usually starts automatically after install via systemd; if not:)
sudo systemctl start ollama
```

Verify all three are reachable:

```bash
# Postgres
pg_isready
# Mongo
mongosh --eval "db.runCommand({ ping: 1 })"
# Ollama
curl -s http://localhost:11434/api/tags
```

(These verification commands are the same on Windows if run from PowerShell or Git Bash, assuming the tools are on PATH.)

### 2.2 Create the Postgres database and enable pgvector

Run once, on either OS (via `psql`):

```bash
psql -U postgres -c "ALTER USER postgres PASSWORD 'postgres';"
psql -U postgres -c "CREATE DATABASE vista;"
psql -U postgres -d vista -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

On Windows, `psql` is on PATH after installation by default; if not, use the full path under `C:\Program Files\PostgreSQL\16\bin\psql.exe`, or open **SQL Shell (psql)** from the Start Menu instead.

### 2.3 Seed mock telemetry data into MongoDB

These `mongosh` commands are identical on both OSes.

```bash
# SG-7: degraded service group (will trigger triage)
mongosh vista_stats --eval 'db.getCollection("stats").insertOne({serviceGroup:"SG-7", cmMac:"00:11:22:33:44:55", timestamp:new Date(), metrics:{rxmer:28.5, flapCount:15, t3Timeouts:7}})'

# SG-3: healthy service group (will be screened out)
mongosh vista_stats --eval 'db.getCollection("stats").insertOne({serviceGroup:"SG-3", cmMac:"AA:BB:CC:DD:EE:01", timestamp:new Date(), metrics:{rxmer:41.0, flapCount:0, t3Timeouts:0}})'

# SG-9: borderline degraded
mongosh vista_stats --eval 'db.getCollection("stats").insertOne({serviceGroup:"SG-9", cmMac:"AA:BB:CC:DD:EE:02", timestamp:new Date(), metrics:{rxmer:33.5, flapCount:9, t3Timeouts:1}})'
```

Confirm the data landed:
```bash
mongosh vista_stats --eval 'db.getCollection("stats").find().toArray()'
```
Expected: three documents with `ISODate` timestamps for SG-7, SG-3, and SG-9.

### 2.4 Start the VISTA application

From `vista/vista`:

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Ubuntu:**
```bash
./mvnw spring-boot:run
```

Wait for the Spring Boot banner and a log line confirming Tomcat started on port 8080. The application is now reachable at `http://localhost:8080/`.

### 2.5 Seed mock RAG incident cases

Once the app is running, seed the knowledge base via its API (identical command on both OSes, run from a separate terminal):

```bash
curl -s -X POST http://localhost:8080/api/kb/cases -H "Content-Type: application/json" -d '{
  "caseId":"INC-001",
  "symptom":"Low RxMER with high flap counts across multiple modems on one service group",
  "classification":"PLANT",
  "resolution":"Field technician repaired damaged upstream coax causing ingress noise"
}'

curl -s -X POST http://localhost:8080/api/kb/cases -H "Content-Type: application/json" -d '{
  "caseId":"INC-002",
  "symptom":"Single modem repeated T3 timeouts with neighbors healthy and RxMER normal",
  "classification":"CPE",
  "resolution":"Replaced faulty cable modem at the subscriber premises"
}'
```

Each call should return `{"status":"embedded","caseId":"INC-..."}`. Confirm the vector store received them:
```bash
psql -U postgres -d vista -c "SELECT COUNT(*) FROM vector_store;"
```
Expected: `2`.

---

## 3. Testing the System

With seed data in place and the app running, work through these scenarios. All commands are OS-agnostic (run from PowerShell, Git Bash, or a Ubuntu terminal).

### 3.1 Chat with tools and memory

Open `http://localhost:8080/` in a browser, pick a model, and ask:
> "What's the latest on SG-7?"

**Look for:** the response should cite the actual seeded numbers (RxMER 28.5, flap count 15), not a generic answer, proving it called the live stats tool rather than guessing. Open the **Agent Logs** panel (top right) to confirm a tool call appears in the live trace.

Then ask a follow-up with no repeated context:
> "How does that compare to SG-3?"

**Look for:** the response should reference SG-7 from the prior turn without you restating it, confirming session memory is working.

### 3.2 On-demand guarded triage

```bash
curl -s -X POST http://localhost:8080/api/triage/guarded \
  -H "Content-Type: application/json" -d '{"serviceGroup":"SG-7","model":"gptoss"}'
```

**Look for:** a JSON response containing `observation`, `interpretation`, and `decision` blocks. Given the seeded precedent, `grounded` should be `true`, and because the likely classification (PLANT) is high-impact, `requiresHumanApproval` should be `true`.

### 3.3 Empty / ungrounded case

```bash
curl -s -X POST http://localhost:8080/api/triage/guarded \
  -H "Content-Type: application/json" -d '{"serviceGroup":"SG-404","model":"gptoss"}'
```

**Look for:** an empty observation, `grounded: false`, and the decision escalating with `requiresHumanApproval: true`. This confirms the system fails safe on unknown input rather than fabricating a confident answer.

### 3.4 Proactive Monitor sweep

Open `http://localhost:8080/monitoring.html` and click **Run Once**.

**Look for:** the status table updates, and the findings box lists SG-7 and SG-9 (both breach the degradation thresholds) but **not** SG-3 (healthy, screened out). Each finding should show a severity tag. Click **Run Once** again immediately, the same findings should *not* duplicate (dedup cooldown), confirming repeat suppression is working.

### 3.5 Observability dashboard

Open `http://localhost:8080/observability.html`.

**Look for:** the model-activity and agent-decision cards reflect the calls made in the prior steps (non-zero call counts, token usage, escalation/groundedness rates). The calibration table will show "no resolved cases yet" unless you've manually confirmed a prediction outcome, that is expected.

### 3.6 MCP tool surface

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list"}'
```

**Look for:** a JSON-RPC response listing the available tools (e.g. `getStatsForServiceGroup`, `getLatestStatsForServiceGroup`), confirming the embedded MCP server is live and exposing the same tools the agent uses internally.

If all six checks pass, the system is fully functional end to end.

---

## 4. Stopping the Services

Shut things down in the reverse order they were started: the application first, then the background services.

### 4.1 Stop the VISTA application

In the terminal running `spring-boot:run`, press `Ctrl+C` (same on both OSes).

### 4.2 Stop background services

#### Windows

```powershell
net stop MongoDB
net stop postgresql-x64-16
```

Ollama can be stopped via its system tray icon ("Quit Ollama"). If you instead started it manually with `ollama serve` in a terminal window (rather than letting it run as the background app), closing that window will stop it, or you can force-kill it from another PowerShell window:

```powershell
taskkill /IM ollama.exe /F
```

#### Ubuntu

```bash
# MongoDB (started with --fork, stop via shutdown command or pkill)
mongosh admin --eval "db.shutdownServer()"
# or, if that fails:
sudo pkill mongod

# PostgreSQL
sudo service postgresql stop

# Ollama (managed by systemd after the official install script)
sudo systemctl stop ollama
sudo systemctl disable ollama

# If you instead started Ollama manually with `ollama serve &` (not via systemd),
# the above will have no effect. Kill it directly:
pkill ollama
```

> Ollama may be running one of two ways depending on how it was started: as a **systemd service** (the default after the official install script) or as a **manual foreground/background process** (if you ran `ollama serve` yourself, e.g. from Section 2.1's fallback). Use `systemctl stop`/`disable` for the former; use `pkill ollama` (Ubuntu) or `taskkill /IM ollama.exe /F` (Windows) for the latter. Running the systemd commands against a manually-started process has no effect, and vice versa, so if `systemctl stop` doesn't actually stop it, fall back to the kill command.

### 4.3 Full reset (optional)

To clear all seeded data and start fresh next time:

```bash
# Clear the vector store (RAG cases)
psql -U postgres -d vista -c "TRUNCATE TABLE vector_store;"

# Clear telemetry
mongosh vista_stats --eval 'db.getCollection("stats").deleteMany({})'
```

Both commands are identical on Windows and Ubuntu. The in-memory prediction log and Monitor findings clear automatically the next time the application restarts.

---

## Quick Reference

| Action | Windows | Ubuntu |
|---|---|---|
| Start Postgres | `net start postgresql-x64-16` | `sudo service postgresql start` |
| Start MongoDB | `net start MongoDB` | `sudo -u mongodb mongod --config /etc/mongod.conf --fork` |
| Start Ollama | `ollama serve` (or tray icon) | `sudo systemctl start ollama` |
| Compile | `.\mvnw.cmd compile` | `./mvnw compile` |
| Run | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Stop app | `Ctrl+C` | `Ctrl+C` |
| Stop Postgres | `net stop postgresql-x64-16` | `sudo service postgresql stop` |
| Stop MongoDB | `net stop MongoDB` | `mongosh admin --eval "db.shutdownServer()"` |
| Stop Ollama (service-managed) | tray icon "Quit" | `sudo systemctl stop ollama && sudo systemctl disable ollama` |
| Stop Ollama (manually started) | `taskkill /IM ollama.exe /F` | `pkill ollama` |
