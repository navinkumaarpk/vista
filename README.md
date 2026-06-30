# VISTA — Virtual Infrastructure Surveillance and Triage Agent

An agentic AI system for vCMTS / cable broadband NOC operations. VISTA runs two
tracks: reactive triage of service-group issues on demand, and proactive
forecasting via a scheduled Monitor agent. It reasons with a Tree-of-Thought
beam search, grounds classifications with RAG over a pgvector store, and operates
inside a safety envelope (guardrails, human-review gates, metrics).

## Stack
- Spring Boot 4 + Spring AI 2.0 (Java 21)
- Models: Ollama (gpt-oss:20b, gemma4:12b) + Anthropic Claude, selectable at runtime
- PostgreSQL + pgvector (RAG), MongoDB (telemetry), embedded MCP server

## Prerequisites
- Java 21, Maven
- PostgreSQL with the `vector` extension, database `vista`
- MongoDB running on 27017, database `vista_stats`
- Ollama with `gpt-oss:20b`, `gemma4:12b`, `nomic-embed-text` pulled
- Env var `ANTHROPIC_API_KEY` for the Claude option

## Run
```
export ANTHROPIC_API_KEY=sk-ant-...
./mvnw spring-boot:run
```
- Chat UI: http://localhost:8080/
- Triage Monitoring: http://localhost:8080/monitoring.html
- Observability Dashboard: http://localhost:8080/observability.html
- MCP endpoint: http://localhost:8080/mcp

For full setup instructions (Windows and Ubuntu), starting background services,
seeding mock data, test scenarios, and teardown steps, see
[`RUNBOOK.md`](./RUNBOOK.md).

## Prompts
Agent prompts and descriptions live in `src/main/resources/prompts/*.md` and are
loaded at startup, so they can be tuned without recompiling.
